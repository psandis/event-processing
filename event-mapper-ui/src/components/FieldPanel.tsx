import type { FieldInfo } from '../types'

interface Props {
  title: string
  fields: Record<string, FieldInfo>
  side: 'source' | 'destination'
  onFieldClick?: (fieldPath: string) => void
  selectedField?: string | null
}

const typeColors: Record<string, string> = {
  string: 'text-green-400',
  integer: 'text-blue-400',
  long: 'text-blue-400',
  double: 'text-purple-400',
  boolean: 'text-amber-400',
  object: 'text-slate-400',
  array: 'text-cyan-400',
}

export function FieldPanel({ title, fields, side: _side, onFieldClick, selectedField }: Props) {
  const fieldEntries = Object.entries(fields)

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden">
      <div className="px-4 py-3 border-b border-slate-800 bg-slate-900/50">
        <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">{title}</h3>
      </div>
      <div className="p-2">
        {fieldEntries.length === 0 ? (
          <div className="text-sm text-slate-600 p-3">No fields discovered</div>
        ) : (
          fieldEntries
            .filter(([, info]) => info.type !== 'object')
            .map(([path, info]) => (
              <button
                key={path}
                onClick={() => onFieldClick?.(path)}
                className={`w-full text-left px-3 py-2 rounded-md text-sm flex items-center justify-between transition-colors ${
                  selectedField === path
                    ? 'bg-indigo-600/20 border border-indigo-500/30'
                    : 'hover:bg-slate-800'
                }`}
              >
                <span className="font-mono text-slate-200">{path}</span>
                <span className={`text-xs font-medium ${typeColors[info.type] || 'text-slate-500'}`}>
                  {info.type}
                </span>
              </button>
            ))
        )}
      </div>
    </div>
  )
}
