import { create } from 'zustand'
import type { FieldMapping, PipelineDefinition, FieldInfo, ConversionType } from '../types'

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

export const useMapperStore = create<MapperState>((set) => ({
  pipeline: null,
  setPipeline: (pipeline) => set({ pipeline, mappings: pipeline.fieldMappings, isDirty: false }),

  sourceFields: {},
  setSourceFields: (fields) => set({ sourceFields: fields }),

  mappings: [],
  addMapping: (source, destination) =>
    set((state) => ({
      mappings: [...state.mappings, { sourceField: source, destinationField: destination }],
      isDirty: true,
    })),
  removeMapping: (source) =>
    set((state) => ({
      mappings: state.mappings.filter((m) => m.sourceField !== source),
      isDirty: true,
    })),
  updateMappingConversion: (source, conversion) =>
    set((state) => ({
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, conversion } : m
      ),
      isDirty: true,
    })),
  updateMappingDefault: (source, defaultValue) =>
    set((state) => ({
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, defaultValue } : m
      ),
      isDirty: true,
    })),
  toggleExcluded: (source) =>
    set((state) => ({
      mappings: state.mappings.map((m) =>
        m.sourceField === source ? { ...m, excluded: !m.excluded } : m
      ),
      isDirty: true,
    })),
  setMappings: (mappings) => set({ mappings, isDirty: true }),

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
