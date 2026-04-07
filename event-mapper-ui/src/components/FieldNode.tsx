import { Handle, Position, type NodeProps } from '@xyflow/react'

const typeColors: Record<string, string> = {
  string: 'border-green-500/40 bg-green-500/5',
  integer: 'border-blue-500/40 bg-blue-500/5',
  long: 'border-blue-500/40 bg-blue-500/5',
  double: 'border-purple-500/40 bg-purple-500/5',
  boolean: 'border-amber-500/40 bg-amber-500/5',
  array: 'border-cyan-500/40 bg-cyan-500/5',
  mapped: 'border-indigo-500/40 bg-indigo-500/5',
}

const typeBadgeColors: Record<string, string> = {
  string: 'text-green-400',
  integer: 'text-blue-400',
  long: 'text-blue-400',
  double: 'text-purple-400',
  boolean: 'text-amber-400',
  array: 'text-cyan-400',
  mapped: 'text-indigo-400',
}

interface FieldNodeData {
  label: string
  fieldType: string
  side: 'source' | 'destination'
  [key: string]: unknown
}

export function FieldNode({ data }: NodeProps) {
  const { label, fieldType, side } = data as FieldNodeData
  const colorClass = typeColors[fieldType] || 'border-slate-700 bg-slate-800/50'
  const badgeColor = typeBadgeColors[fieldType] || 'text-slate-500'

  return (
    <div className={`px-4 py-2 rounded-lg border ${colorClass} min-w-[200px] flex items-center justify-between gap-3`}>
      {side === 'destination' && (
        <Handle type="target" position={Position.Left} className="!bg-indigo-500 !w-2.5 !h-2.5 !border-0" />
      )}

      <span className="font-mono text-xs text-slate-200 truncate">{label}</span>
      <span className={`text-[10px] font-medium ${badgeColor} uppercase`}>{fieldType}</span>

      {side === 'source' && (
        <Handle type="source" position={Position.Right} className="!bg-indigo-500 !w-2.5 !h-2.5 !border-0" />
      )}
    </div>
  )
}
