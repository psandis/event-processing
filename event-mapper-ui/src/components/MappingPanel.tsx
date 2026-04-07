import { useMapperStore } from '../store/mapperStore'
import type { ConversionType } from '../types'

const conversionOptions: ConversionType[] = [
  'NONE', 'TO_STRING', 'TO_INTEGER', 'TO_LONG', 'TO_DOUBLE',
  'TO_BOOLEAN', 'TO_TIMESTAMP', 'TO_UPPER', 'TO_LOWER', 'FLATTEN', 'MASK',
]

interface Props {
  selectedMapping: string | null
  onClose: () => void
  onRemove: (source: string) => void
  onConversionChange: (source: string, conversion: ConversionType) => void
}

export function MappingPanel({ selectedMapping, onClose, onRemove, onConversionChange }: Props) {
  const { mappings } = useMapperStore()

  const mapping = selectedMapping
    ? mappings.find((m) => m.sourceField === selectedMapping)
    : null

  if (!mapping) {
    return (
      <div className="w-72 border-l border-slate-800 bg-slate-950 p-4">
        <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
          Mapping Details
        </div>
        <div className="text-sm text-slate-600 text-center py-8">
          Click a connection line to view and edit mapping details
        </div>
      </div>
    )
  }

  return (
    <div className="w-72 border-l border-slate-800 bg-slate-950 p-4">
      <div className="flex items-center justify-between mb-4">
        <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
          Mapping Details
        </div>
        <button onClick={onClose} className="text-slate-600 hover:text-slate-400 text-sm">&times;</button>
      </div>

      <div className="space-y-4">
        <div>
          <div className="text-xs text-slate-500 mb-1">Source</div>
          <div className="font-mono text-sm text-slate-300 bg-slate-900 px-3 py-2 rounded-lg">
            {mapping.sourceField}
          </div>
        </div>

        <div className="text-center text-slate-600">&darr;</div>

        <div>
          <div className="text-xs text-slate-500 mb-1">Destination</div>
          <div className="font-mono text-sm text-slate-300 bg-slate-900 px-3 py-2 rounded-lg">
            {mapping.destinationField}
          </div>
        </div>

        <div>
          <div className="text-xs text-slate-500 mb-1">Conversion</div>
          <select
            value={mapping.conversion || 'NONE'}
            onChange={(e) => onConversionChange(mapping.sourceField, e.target.value as ConversionType)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-300 focus:outline-none focus:border-indigo-500"
          >
            {conversionOptions.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <button
          onClick={() => onRemove(mapping.sourceField)}
          className="w-full px-3 py-2 bg-red-950/30 hover:bg-red-950/50 text-red-400 text-sm rounded-lg transition-colors border border-red-900/30"
        >
          Remove Mapping
        </button>
      </div>
    </div>
  )
}
