import { useState } from 'react'
import { useMapperStore } from '../store/mapperStore'
import { api } from '../api/client'

export function PreviewPanel() {
  const { pipeline, previewOutput, setPreviewInput, setPreviewOutput } = useMapperStore()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sampleJson, setSampleJson] = useState('')

  const handleLoadSample = async () => {
    if (!pipeline) return
    try {
      setLoading(true)
      setError(null)
      const samples = await api.getSampleEvents(pipeline.sourceTopic, 1)
      if (samples.length > 0) {
        const event = samples[0] as Record<string, unknown>
        const payload = event.payload || event
        setPreviewInput(payload as object)
        setSampleJson(JSON.stringify(payload, null, 2))
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load sample')
    } finally {
      setLoading(false)
    }
  }

  const handleTest = async () => {
    if (!pipeline || !sampleJson.trim()) return
    try {
      setLoading(true)
      setError(null)
      const input = JSON.parse(sampleJson)
      setPreviewInput(input)
      const result = await api.testMapping(pipeline.name, input)
      setPreviewOutput(result)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Test failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="border-t border-slate-800 bg-slate-950">
      <div className="flex items-center justify-between px-6 py-3 border-b border-slate-800">
        <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Live Preview</h3>
        <div className="flex gap-2">
          <button
            onClick={handleLoadSample}
            disabled={loading || !pipeline}
            className="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded transition-colors disabled:opacity-50"
          >
            Load Sample
          </button>
          <button
            onClick={handleTest}
            disabled={loading || !pipeline || !sampleJson.trim()}
            className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded transition-colors disabled:opacity-50"
          >
            {loading ? 'Testing...' : 'Test Mapping'}
          </button>
        </div>
      </div>

      {error && (
        <div className="px-6 py-2 bg-red-950/30 text-red-400 text-sm">{error}</div>
      )}

      <div className="grid grid-cols-2 gap-0 divide-x divide-slate-800">
        <div className="p-4">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Input</div>
          <textarea
            value={sampleJson}
            onChange={(e) => setSampleJson(e.target.value)}
            placeholder="Paste sample JSON or click Load Sample"
            className="w-full h-48 bg-slate-900 border border-slate-800 rounded-lg p-3 font-mono text-xs text-slate-300 placeholder-slate-700 resize-none focus:outline-none focus:border-indigo-500"
          />
        </div>
        <div className="p-4">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Output</div>
          <pre className="w-full h-48 bg-slate-900 border border-slate-800 rounded-lg p-3 font-mono text-xs text-emerald-400 overflow-auto">
            {previewOutput ? JSON.stringify(previewOutput, null, 2) : 'Run a test to see the output'}
          </pre>
        </div>
      </div>
    </div>
  )
}
