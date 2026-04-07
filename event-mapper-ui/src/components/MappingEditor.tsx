import { useState } from 'react'
import { useMapperStore } from '../store/mapperStore'
import { FieldPanel } from './FieldPanel'
import type { ConversionType, FieldInfo } from '../types'

const conversionOptions: ConversionType[] = [
  'NONE', 'TO_STRING', 'TO_INTEGER', 'TO_LONG', 'TO_DOUBLE',
  'TO_BOOLEAN', 'TO_TIMESTAMP', 'TO_UPPER', 'TO_LOWER', 'FLATTEN', 'MASK',
]

export function MappingEditor() {
  const {
    sourceFields,
    mappings,
    addMapping,
    removeMapping,
    updateMappingConversion,
    toggleExcluded,
  } = useMapperStore()

  const [selectedSource, setSelectedSource] = useState<string | null>(null)
  const [newDestField, setNewDestField] = useState('')

  const handleSourceClick = (path: string) => {
    setSelectedSource(path)
  }

  const handleCreateMapping = () => {
    if (selectedSource && newDestField.trim()) {
      addMapping(selectedSource, newDestField.trim())
      setSelectedSource(null)
      setNewDestField('')
    }
  }

  const mappedDestFields: Record<string, FieldInfo> = {}
  for (const m of mappings) {
    if (!m.excluded) {
      const sourceInfo = sourceFields[m.sourceField]
      mappedDestFields[m.destinationField] = {
        path: m.destinationField,
        type: sourceInfo?.type || 'string',
        occurrences: 1,
      }
    }
  }

  return (
    <div className="flex-1 flex flex-col gap-4 p-6 overflow-auto">
      <div className="grid grid-cols-[1fr_auto_1fr] gap-6 items-start">
        {/* Source fields */}
        <FieldPanel
          title="Source Fields"
          fields={sourceFields}
          side="source"
          onFieldClick={handleSourceClick}
          selectedField={selectedSource}
        />

        {/* Mapping connections */}
        <div className="w-80 space-y-3 pt-12">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">
            Field Mappings
          </div>

          {mappings.map((m) => (
            <div
              key={m.sourceField}
              className={`p-3 rounded-lg border text-sm ${
                m.excluded
                  ? 'bg-red-950/20 border-red-900/30 opacity-60'
                  : 'bg-slate-900 border-slate-800'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-xs text-slate-400">{m.sourceField}</span>
                <span className="text-slate-600 mx-2">&rarr;</span>
                <span className="font-mono text-xs text-slate-200">{m.destinationField}</span>
              </div>
              <div className="flex items-center gap-2">
                <select
                  value={m.conversion || 'NONE'}
                  onChange={(e) => updateMappingConversion(m.sourceField, e.target.value as ConversionType)}
                  className="flex-1 bg-slate-800 border border-slate-700 rounded px-2 py-1 text-xs text-slate-300"
                  disabled={m.excluded}
                >
                  {conversionOptions.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
                <button
                  onClick={() => toggleExcluded(m.sourceField)}
                  className={`text-xs px-2 py-1 rounded ${
                    m.excluded
                      ? 'bg-red-600/20 text-red-400'
                      : 'bg-slate-800 text-slate-500 hover:text-red-400'
                  }`}
                  title={m.excluded ? 'Include field' : 'Exclude field'}
                >
                  {m.excluded ? 'excluded' : 'exclude'}
                </button>
                <button
                  onClick={() => removeMapping(m.sourceField)}
                  className="text-xs text-slate-600 hover:text-red-400 px-1"
                  title="Remove mapping"
                >
                  &times;
                </button>
              </div>
            </div>
          ))}

          {selectedSource && (
            <div className="p-3 bg-indigo-950/30 border border-indigo-500/30 rounded-lg">
              <div className="text-xs text-indigo-400 mb-2">
                Map "{selectedSource}" to:
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newDestField}
                  onChange={(e) => setNewDestField(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCreateMapping()}
                  placeholder="destination.field"
                  className="flex-1 bg-slate-900 border border-slate-700 rounded px-2 py-1 text-sm font-mono text-slate-200 placeholder-slate-600"
                  autoFocus
                />
                <button
                  onClick={handleCreateMapping}
                  disabled={!newDestField.trim()}
                  className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:text-slate-500 text-white text-sm rounded transition-colors"
                >
                  Map
                </button>
              </div>
            </div>
          )}

          {mappings.length === 0 && !selectedSource && (
            <div className="text-sm text-slate-600 text-center py-8">
              Click a source field to start mapping
            </div>
          )}
        </div>

        {/* Destination fields (derived from mappings) */}
        <FieldPanel
          title="Destination Fields"
          fields={mappedDestFields}
          side="destination"
        />
      </div>
    </div>
  )
}
