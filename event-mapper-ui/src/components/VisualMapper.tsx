import { useCallback, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
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

const SOURCE_X = 50
const DEST_X = 600
const NODE_GAP = 60
const HEADER_OFFSET = 40

export function VisualMapper() {
  const {
    sourceFields, mappings, addMapping, removeMapping,
    updateMappingConversion, destinationFields, addDestinationField,
  } = useMapperStore()
  const [selectedMapping, setSelectedMapping] = useState<string | null>(null)
  const [newDestField, setNewDestField] = useState('')
  const [showAddDest, setShowAddDest] = useState(false)

  const sourceEntries = useMemo(() =>
    Object.entries(sourceFields).filter(([, info]) => info.type !== 'object'),
    [sourceFields]
  )

  // Stable destination field list: preserves order, never removes on unmap
  const allDestFields = useMemo(() => {
    const ordered: string[] = [...destinationFields]
    for (const m of mappings) {
      if (!m.excluded && !ordered.includes(m.destinationField)) {
        ordered.push(m.destinationField)
      }
    }
    return ordered
  }, [mappings, destinationFields])

  const nodes: Node[] = useMemo(() => {
    const sourceHeader: Node = {
      id: 'header-source',
      type: 'default',
      position: { x: SOURCE_X, y: 0 },
      data: { label: 'SOURCE FIELDS' },
      selectable: false,
      draggable: false,
      style: {
        background: 'transparent',
        border: 'none',
        color: '#64748b',
        fontSize: '11px',
        fontWeight: '700',
        letterSpacing: '0.1em',
        width: 200,
        pointerEvents: 'none' as const,
      },
    }

    const destHeader: Node = {
      id: 'header-dest',
      type: 'default',
      position: { x: DEST_X, y: 0 },
      data: { label: 'DESTINATION FIELDS' },
      selectable: false,
      draggable: false,
      style: {
        background: 'transparent',
        border: 'none',
        color: '#64748b',
        fontSize: '11px',
        fontWeight: '700',
        letterSpacing: '0.1em',
        width: 200,
        pointerEvents: 'none' as const,
      },
    }

    const sourceNodes: Node[] = sourceEntries.map(([path, info], i) => ({
      id: `source-${path}`,
      type: 'field',
      position: { x: SOURCE_X, y: HEADER_OFFSET + i * NODE_GAP },
      data: { label: path, fieldType: info.type, side: 'source' },
      sourcePosition: Position.Right,
      draggable: false,
    }))

    const destNodes: Node[] = allDestFields.map((path, i) => ({
      id: `dest-${path}`,
      type: 'field',
      position: { x: DEST_X, y: HEADER_OFFSET + i * NODE_GAP },
      data: { label: path, fieldType: 'mapped', side: 'destination' },
      targetPosition: Position.Left,
      draggable: false,
    }))

    return [sourceHeader, destHeader, ...sourceNodes, ...destNodes]
  }, [sourceEntries, allDestFields])

  const edges: Edge[] = useMemo(() =>
    mappings
      .filter((m) => !m.excluded)
      .map((m) => ({
        id: `edge-${m.sourceField}`,
        source: `source-${m.sourceField}`,
        target: `dest-${m.destinationField}`,
        animated: m.conversion !== undefined && m.conversion !== 'NONE',
        style: {
          stroke: selectedMapping === m.sourceField ? '#818cf8' : '#475569',
          strokeWidth: selectedMapping === m.sourceField ? 2.5 : 1.5,
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: selectedMapping === m.sourceField ? '#818cf8' : '#475569',
          width: 16,
          height: 16,
        },
        label: m.conversion && m.conversion !== 'NONE' ? m.conversion : undefined,
        labelStyle: {
          fill: selectedMapping === m.sourceField ? '#c7d2fe' : '#94a3b8',
          fontSize: 9,
          fontFamily: 'monospace',
          fontWeight: selectedMapping === m.sourceField ? '600' : '400',
        },
        labelBgStyle: { fill: '#0f172a', fillOpacity: 0.95 },
        labelBgPadding: [6, 3] as [number, number],
        labelBgBorderRadius: 4,
      })),
    [mappings, selectedMapping]
  )

  const onConnect: OnConnect = useCallback((connection: Connection) => {
    if (!connection.source || !connection.target) return
    const sourceField = connection.source.replace('source-', '')
    const destField = connection.target.replace('dest-', '')

    // Check if this source is already mapped
    const existing = useMapperStore.getState().mappings.find((m) => m.sourceField === sourceField)
    if (existing) return

    addMapping(sourceField, destField)
  }, [addMapping])

  const onEdgeClick = useCallback((_: React.MouseEvent, edge: Edge) => {
    const sourceField = edge.id.replace('edge-', '')
    setSelectedMapping(selectedMapping === sourceField ? null : sourceField)
  }, [selectedMapping])

  const handleAddDestField = () => {
    if (newDestField.trim()) {
      addDestinationField(newDestField.trim())
      setNewDestField('')
      setShowAddDest(false)
    }
  }

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
          fitViewOptions={{ padding: 0.4 }}
          proOptions={{ hideAttribution: true }}
          style={{ background: '#0f172a' }}
          minZoom={0.5}
          maxZoom={1.5}
          defaultEdgeOptions={{ type: 'smoothstep' }}
        >
          <Background color="#1e293b" gap={20} />
          <Controls
            showInteractive={false}
            style={{ background: '#1e293b', borderColor: '#334155', borderRadius: '8px' }}
          />
        </ReactFlow>

        {/* Add destination field button */}
        <div className="absolute top-3 right-80 z-10">
          {showAddDest ? (
            <div className="bg-slate-900 border border-slate-700 rounded-lg p-3 shadow-xl flex gap-2">
              <input
                type="text"
                value={newDestField}
                onChange={(e) => setNewDestField(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAddDestField()}
                placeholder="field.name"
                className="bg-slate-800 border border-slate-700 rounded px-2 py-1 text-sm font-mono text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 w-48"
                autoFocus
              />
              <button
                onClick={handleAddDestField}
                disabled={!newDestField.trim()}
                className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 text-white text-xs font-medium rounded transition-colors"
              >
                Add
              </button>
              <button
                onClick={() => { setShowAddDest(false); setNewDestField('') }}
                className="text-slate-500 hover:text-slate-300 text-sm px-1"
              >
                &times;
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowAddDest(true)}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-400 text-xs font-medium rounded-lg transition-colors"
            >
              + Add Destination Field
            </button>
          )}
        </div>

        {/* Instructions */}
        {mappings.length === 0 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-sm text-slate-600 bg-slate-900/80 px-4 py-2 rounded-lg z-10">
            Drag from a source field handle to a destination field to create a mapping
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
