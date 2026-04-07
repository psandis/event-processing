import { useEffect, useState } from 'react'
import { useMapperStore } from '../store/mapperStore'
import { api } from '../api/client'
import type { PipelineDefinition } from '../types'

interface Props {
  onBack: () => void
  onSaved: (pipeline: PipelineDefinition) => void
}

export function PipelineToolbar({ onBack, onSaved }: Props) {
  const { pipeline, mappings, isDirty, canUndo, undo } = useMapperStore()
  const [saving, setSaving] = useState(false)
  const [deploying, setDeploying] = useState(false)
  const [showDeployConfirm, setShowDeployConfirm] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'z' && canUndo) {
        e.preventDefault()
        undo()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [canUndo, undo])

  if (!pipeline) return null

  const isActive = pipeline.state === 'ACTIVE'
  const isDraft = pipeline.state === 'DRAFT'

  const handleSave = async () => {
    if (!isDraft) return
    try {
      setSaving(true)
      setError(null)
      const updated = await api.updatePipeline(pipeline.name, {
        ...pipeline,
        fieldMappings: mappings,
      })
      onSaved(updated)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const handleDeploy = async () => {
    try {
      setDeploying(true)
      setError(null)
      const deployed = await api.deploy(pipeline.name, pipeline.version)
      onSaved(deployed)
      setShowDeployConfirm(false)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Deploy failed')
    } finally {
      setDeploying(false)
    }
  }

  return (
    <div className="border-b border-slate-800 bg-slate-900/50">
      <div className="flex items-center justify-between px-6 py-3">
        <div className="flex items-center gap-4">
          <button
            onClick={onBack}
            className="text-slate-500 hover:text-slate-300 text-sm transition-colors"
          >
            &larr; Pipelines
          </button>
          <div>
            <span className="font-semibold text-slate-200">{pipeline.name}</span>
            <span className="text-slate-500 text-sm ml-2">v{pipeline.version}</span>
            <span className={`ml-2 text-xs px-2 py-0.5 rounded ${
              isActive ? 'bg-emerald-500/10 text-emerald-400' :
              isDraft ? 'bg-amber-500/10 text-amber-400' :
              'bg-slate-500/10 text-slate-400'
            }`}>
              {pipeline.state}
            </span>
            {isDirty && <span className="ml-2 text-xs text-amber-400">unsaved</span>}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={undo}
            disabled={!canUndo}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-400 text-sm rounded-lg transition-colors disabled:opacity-30"
            title="Undo (Ctrl+Z)"
          >
            Undo
          </button>

          {isDraft && (
            <>
              <button
                onClick={handleSave}
                disabled={saving || !isDirty}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button
                onClick={() => setShowDeployConfirm(true)}
                disabled={deploying || isDirty}
                className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
              >
                Deploy
              </button>
            </>
          )}

          {isActive && (
            <div className="text-xs text-slate-500">
              Active pipeline. Create a draft version to edit.
            </div>
          )}
        </div>
      </div>

      {error && (
        <div className="px-6 py-2 bg-red-950/30 text-red-400 text-sm border-t border-red-900/30">
          {error}
        </div>
      )}

      {showDeployConfirm && (
        <div className="px-6 py-3 bg-amber-950/20 border-t border-amber-900/30">
          <div className="flex items-center justify-between">
            <div className="text-sm text-amber-300">
              Deploy will start a new engine instance with these mappings.
              {isActive && ' The current active pipeline will be paused.'}
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDeployConfirm(false)}
                className="px-3 py-1 text-sm text-slate-400 hover:text-slate-200"
              >
                Cancel
              </button>
              <button
                onClick={handleDeploy}
                disabled={deploying}
                className="px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded transition-colors"
              >
                {deploying ? 'Deploying...' : 'Confirm Deploy'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
