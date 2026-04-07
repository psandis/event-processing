import { useState } from 'react'
import { PipelineList } from './components/PipelineList'
import { VisualMapper } from './components/VisualMapper'
import { PreviewPanel } from './components/PreviewPanel'
import { PipelineToolbar } from './components/PipelineToolbar'
import { useMapperStore } from './store/mapperStore'
import { api } from './api/client'
import type { PipelineDefinition } from './types'
import './app.css'

type View = 'list' | 'editor' | 'new'

export default function App() {
  const [view, setView] = useState<View>('list')
  const { setPipeline, setSourceFields } = useMapperStore()

  const handleSelectPipeline = async (pipeline: PipelineDefinition) => {
    setPipeline(pipeline)

    try {
      const schema = await api.discoverSchema(pipeline.sourceTopic)
      setSourceFields(schema)
    } catch {
      // Fallback: derive source fields from existing mappings
      const fallback: Record<string, { path: string; type: string; occurrences: number }> = {}
      for (const m of pipeline.fieldMappings) {
        fallback[m.sourceField] = { path: m.sourceField, type: 'string', occurrences: 1 }
      }
      setSourceFields(fallback)
    }

    setView('editor')
  }

  const handleSaved = (pipeline: PipelineDefinition) => {
    setPipeline(pipeline)
  }

  const handleBack = () => {
    setView('list')
  }

  const handleNew = () => {
    setView('new')
  }

  return (
    <div className="h-screen flex flex-col">
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-sm px-6 py-3 flex items-center justify-between">
        <h1 className="text-lg font-semibold tracking-tight text-slate-100">Event Mapper</h1>
        <div className="text-xs text-slate-600">Event Processing Platform</div>
      </header>

      {view === 'list' && (
        <div className="flex-1 overflow-auto">
          <PipelineList onSelect={handleSelectPipeline} onNew={handleNew} />
        </div>
      )}

      {view === 'editor' && (
        <>
          <PipelineToolbar onBack={handleBack} onSaved={handleSaved} />
          <VisualMapper />
          <PreviewPanel />
        </>
      )}

      {view === 'new' && (
        <NewPipelineForm
          onCreated={(p) => handleSelectPipeline(p)}
          onCancel={() => setView('list')}
        />
      )}
    </div>
  )
}

function NewPipelineForm({ onCreated, onCancel }: { onCreated: (p: PipelineDefinition) => void, onCancel: () => void }) {
  const [name, setName] = useState('')
  const [sourceTopic, setSourceTopic] = useState('')
  const [destinationTopic, setDestinationTopic] = useState('')
  const [topics, setTopics] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [topicsLoaded, setTopicsLoaded] = useState(false)

  const loadTopics = async () => {
    if (topicsLoaded) return
    try {
      const t = await api.listTopics()
      setTopics(t)
      setTopicsLoaded(true)
    } catch {
      // Topics might not be available
    }
  }

  const handleCreate = async () => {
    if (!name.trim() || !sourceTopic.trim() || !destinationTopic.trim()) return
    try {
      setLoading(true)
      setError(null)
      const pipeline = await api.createPipeline({
        name: name.trim(),
        sourceTopic: sourceTopic.trim(),
        destinationTopic: destinationTopic.trim(),
        fieldMappings: [],
      })
      onCreated(pipeline)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create pipeline')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-xl p-8">
        <h2 className="text-xl font-semibold mb-6">New Pipeline</h2>

        {error && <div className="mb-4 p-3 bg-red-950/30 text-red-400 text-sm rounded-lg">{error}</div>}

        <div className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="orders-to-warehouse"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">Source Topic</label>
            <input
              type="text"
              value={sourceTopic}
              onChange={(e) => setSourceTopic(e.target.value)}
              onFocus={loadTopics}
              placeholder="events.raw"
              list="source-topics"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
            />
            <datalist id="source-topics">
              {topics.map((t) => <option key={t} value={t} />)}
            </datalist>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">Destination Topic</label>
            <input
              type="text"
              value={destinationTopic}
              onChange={(e) => setDestinationTopic(e.target.value)}
              onFocus={loadTopics}
              placeholder="warehouse.fulfillment"
              list="dest-topics"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
            />
            <datalist id="dest-topics">
              {topics.map((t) => <option key={t} value={t} />)}
            </datalist>
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-8">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm text-slate-400 hover:text-slate-200 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleCreate}
            disabled={loading || !name.trim() || !sourceTopic.trim() || !destinationTopic.trim()}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
          >
            {loading ? 'Creating...' : 'Create Pipeline'}
          </button>
        </div>
      </div>
    </div>
  )
}
