import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { PipelineDefinition } from '../types'

const stateColors: Record<string, string> = {
  DRAFT: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  ACTIVE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  PAUSED: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
  DEPLOYING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
}

interface Props {
  onSelect: (pipeline: PipelineDefinition) => void
  onNew: () => void
}

export function PipelineList({ onSelect, onNew }: Props) {
  const [pipelines, setPipelines] = useState<PipelineDefinition[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.listPipelines()
      .then(setPipelines)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-slate-500 p-8">Loading pipelines...</div>
  if (error) return <div className="text-red-400 p-8">Failed to load pipelines: {error}</div>

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold">Pipelines</h2>
        <button
          onClick={onNew}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors"
        >
          New Pipeline
        </button>
      </div>

      {pipelines.length === 0 ? (
        <div className="text-center py-16 text-slate-500">
          <p className="text-lg mb-2">No pipelines yet</p>
          <p className="text-sm">Create your first pipeline to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {pipelines.map((p) => (
            <button
              key={`${p.name}-v${p.version}`}
              onClick={() => onSelect(p)}
              className="w-full text-left p-4 bg-slate-900 border border-slate-800 rounded-lg hover:border-slate-700 transition-colors"
            >
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-medium">{p.name}</span>
                  <span className="text-slate-500 text-sm ml-2">v{p.version}</span>
                </div>
                <span className={`text-xs font-medium px-2 py-1 rounded-md border ${stateColors[p.state] || ''}`}>
                  {p.state}
                </span>
              </div>
              <div className="text-sm text-slate-500 mt-1">
                {p.sourceTopic} &rarr; {p.destinationTopic}
                {p.fieldMappings && (
                  <span className="ml-2">({p.fieldMappings.length} mappings)</span>
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
