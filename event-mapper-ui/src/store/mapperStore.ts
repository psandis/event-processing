import { create } from 'zustand'
import type { FieldMapping, PipelineDefinition, FieldInfo, ConversionType } from '../types'

const MAX_HISTORY = 50

interface MapperState {
  // Pipeline
  pipeline: PipelineDefinition | null
  setPipeline: (pipeline: PipelineDefinition) => void

  // Source schema
  sourceFields: Record<string, FieldInfo>
  setSourceFields: (fields: Record<string, FieldInfo>) => void

  // Mappings
  mappings: FieldMapping[]
  addMapping: (source: string, destination: string) => void
  removeMapping: (source: string) => void
  updateMappingConversion: (source: string, conversion: ConversionType) => void
  updateMappingDefault: (source: string, defaultValue: string) => void
  toggleExcluded: (source: string) => void
  setMappings: (mappings: FieldMapping[]) => void

  // Undo
  history: FieldMapping[][]
  undo: () => void
  canUndo: boolean

  // Destination fields (user-defined)
  destinationFields: string[]
  addDestinationField: (field: string) => void
  removeDestinationField: (field: string) => void
  setDestinationFields: (fields: string[]) => void

  // Preview
  previewInput: object | null
  previewOutput: object | null
  setPreviewInput: (input: object) => void
  setPreviewOutput: (output: object) => void

  // UI state
  isDirty: boolean
  setDirty: (dirty: boolean) => void
}

function pushHistory(state: MapperState): Partial<MapperState> {
  const history = [...state.history, state.mappings.map((m) => ({ ...m }))]
  if (history.length > MAX_HISTORY) history.shift()
  return { history, canUndo: true }
}

export const useMapperStore = create<MapperState>((set) => ({
  pipeline: null,
  setPipeline: (pipeline) => set({
    pipeline,
    mappings: pipeline.fieldMappings,
    history: [],
    canUndo: false,
    isDirty: false,
  }),

  sourceFields: {},
  setSourceFields: (fields) => set({ sourceFields: fields }),

  mappings: [],
  addMapping: (source, destination) =>
    set((state) => ({
      ...pushHistory(state),
      mappings: [...state.mappings, { sourceField: source, destinationField: destination }],
      isDirty: true,
    })),
  removeMapping: (source) =>
    set((state) => {
      const removed = state.mappings.find((m) => m.sourceField === source)
      const keepDest = removed && !removed.excluded
        ? [...new Set([...state.destinationFields, removed.destinationField])]
        : state.destinationFields
      return {
        ...pushHistory(state),
        mappings: state.mappings.filter((m) => m.sourceField !== source),
        destinationFields: keepDest,
        isDirty: true,
      }
    }),
  updateMappingConversion: (source, conversion) =>
    set((state) => ({
      ...pushHistory(state),
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, conversion } : m
      ),
      isDirty: true,
    })),
  updateMappingDefault: (source, defaultValue) =>
    set((state) => ({
      ...pushHistory(state),
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, defaultValue } : m
      ),
      isDirty: true,
    })),
  toggleExcluded: (source) =>
    set((state) => ({
      ...pushHistory(state),
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, excluded: !m.excluded } : m
      ),
      isDirty: true,
    })),
  setMappings: (mappings) => set((state) => ({
    ...pushHistory(state),
    mappings,
    isDirty: true,
  })),

  history: [],
  canUndo: false,
  undo: () =>
    set((state) => {
      if (state.history.length === 0) return state
      const history = [...state.history]
      const previous = history.pop()!
      return { mappings: previous, history, canUndo: history.length > 0, isDirty: true }
    }),

  destinationFields: [],
  addDestinationField: (field) =>
    set((state) => ({
      destinationFields: [...state.destinationFields, field],
    })),
  removeDestinationField: (field) =>
    set((state) => ({
      destinationFields: state.destinationFields.filter((f) => f !== field),
    })),
  setDestinationFields: (fields) => set({ destinationFields: fields }),

  previewInput: null,
  previewOutput: null,
  setPreviewInput: (input) => set({ previewInput: input }),
  setPreviewOutput: (output) => set({ previewOutput: output }),

  isDirty: false,
  setDirty: (dirty) => set({ isDirty: dirty }),
}))
