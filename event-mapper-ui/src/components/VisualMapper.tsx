import { useCallback, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  type Node,
  type Edge,
  type Connection,
  type OnConnect,
  type NodeTypes,
  Position,
  MarkerType,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useMapperStore } from '../store/mapperStore'
import { FieldNode } from './FieldNode'
import { MappingPanel } from './MappingPanel'

const nodeTypes: NodeTypes = {
  field: FieldNode,
}

export function VisualMapper() {
  const { sourceFields, mappings, addMapping, removeMapping, updateMappingConversion } = useMapperStore()
  const [selectedMapping, setSelectedMapping] = useState<string | null>(null)
  const [destFieldInput, setDestFieldInput] = useState('')
  const [pendingSource, setPendingSource] = useState<string | null>(null)

  const sourceEntries = useMemo(() =>
    Object.entries(sourceFields).filter(([, info]) => info.type !== 'object'),
    [sourceFields]
  )

  const destEntries = useMemo(() => {
    const fields: string[] = []
    for (const m of mappings) {
      if (!m.excluded && !fields.includes(m.destinationField)) {
        fields.push(m.destinationField)
      }
    }
    return fields
  }, [mappings])

  const nodes: Node[] = useMemo(() => {
    const sourceNodes: Node[] = sourceEntries.map(([path, info], i) => ({
      id: `source-${path}`,
      type: 'field',
      position: { x: 0, y: i * 56 },
      data: { label: path, fieldType: info.type, side: 'source' },
      sourcePosition: Position.Right,
    }))

    const destNodes: Node[] = destEntries.map((path, i) => ({
      id: `dest-${path}`,
      type: 'field',
      position: { x: 500, y: i * 56 },
      data: { label: path, fieldType: 'mapped', side: 'destination' },
      targetPosition: Position.Left,
    }))

    return [...sourceNodes, ...destNodes]
  }, [sourceEntries, destEntries])

  const edges: Edge[] = useMemo(() =>
    mappings
      .filter((m) => !m.excluded)
      .map((m) => ({
        id: `edge-${m.sourceField}`,
        source: `source-${m.sourceField}`,
        target: `dest-${m.destinationField}`,
        animated: false,
        style: {
          stroke: selectedMapping === m.sourceField ? '#818cf8' : '#475569',
          strokeWidth: selectedMapping === m.sourceField ? 2 : 1,
        },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#475569' },
        label: m.conversion && m.conversion !== 'NONE' ? m.conversion : undefined,
        labelStyle: { fill: '#94a3b8', fontSize: 10, fontFamily: 'monospace' },
        labelBgStyle: { fill: '#0f172a', fillOpacity: 0.9 },
      })),
    [mappings, selectedMapping]
  )

  const onConnect: OnConnect = useCallback((connection: Connection) => {
    if (!connection.source || !connection.target) return
    const sourceField = connection.source.replace('source-', '')
    setPendingSource(sourceField)
  }, [])

  const handleCreateMapping = () => {
    if (pendingSource && destFieldInput.trim()) {
      addMapping(pendingSource, destFieldInput.trim())
      setPendingSource(null)
      setDestFieldInput('')
    }
  }

  const onEdgeClick = useCallback((_: React.MouseEvent, edge: Edge) => {
    const sourceField = edge.id.replace('edge-', '')
    setSelectedMapping(selectedMapping === sourceField ? null : sourceField)
  }, [selectedMapping])

  return (
    <div className="flex-1 flex">
      <div className="flex-1 relative">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          onConnect={onConnect}
          onEdgeClick={onEdgeClick}
          fitView
          fitViewOptions={{ padding: 0.3 }}
          proOptions={{ hideAttribution: true }}
          style={{ background: '#0f172a' }}
        >
          <Background color="#1e293b" gap={20} />
        </ReactFlow>

        {pendingSource && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-slate-900 border border-indigo-500/30 rounded-xl p-4 shadow-xl z-10">
            <div className="text-xs text-indigo-400 mb-2">
              Map "{pendingSource}" to:
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                value={destFieldInput}
                onChange={(e) => setDestFieldInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleCreateMapping()}
                placeholder="destination.field"
                className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm font-mono text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 w-64"
                autoFocus
              />
              <button
                onClick={handleCreateMapping}
                disabled={!destFieldInput.trim()}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 text-white text-sm font-medium rounded-lg transition-colors"
              >
                Map
              </button>
              <button
                onClick={() => { setPendingSource(null); setDestFieldInput('') }}
                className="px-3 py-2 text-slate-500 hover:text-slate-300 text-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      <MappingPanel
        selectedMapping={selectedMapping}
        onClose={() => setSelectedMapping(null)}
        onRemove={(source) => { removeMapping(source); setSelectedMapping(null) }}
        onConversionChange={(source, conversion) => updateMappingConversion(source, conversion)}
      />
    </div>
  )
}
