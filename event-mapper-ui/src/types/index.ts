export interface FieldMapping {
  sourceField: string
  destinationField: string
  conversion?: ConversionType
  defaultValue?: string
  excluded?: boolean
}

export type ConversionType =
  | 'NONE'
  | 'TO_STRING'
  | 'TO_INTEGER'
  | 'TO_LONG'
  | 'TO_DOUBLE'
  | 'TO_BOOLEAN'
  | 'TO_TIMESTAMP'
  | 'TO_UPPER'
  | 'TO_LOWER'
  | 'FLATTEN'
  | 'MASK'

export type PipelineState = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'DEPLOYING'

export interface ErrorHandling {
  retries: number
  backoffMs: number
  deadLetterTopic: string
}

export interface PipelineDefinition {
  name: string
  description?: string
  sourceTopic: string
  destinationTopic: string
  version: number
  state: PipelineState
  fieldMappings: FieldMapping[]
  errorHandling?: ErrorHandling
}

export interface FieldInfo {
  path: string
  type: string
  occurrences: number
}

export type SchemaMap = Record<string, FieldInfo>
