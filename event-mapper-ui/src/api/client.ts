import type { PipelineDefinition, SchemaMap } from '../types'

const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.detail || `Request failed: ${res.status}`)
  }
  return res.json()
}

export const api = {
  // Pipelines
  listPipelines: () => request<PipelineDefinition[]>('/pipelines'),
  getPipeline: (name: string) => request<PipelineDefinition>(`/pipelines/${name}`),
  getPipelineVersions: (name: string) => request<PipelineDefinition[]>(`/pipelines/${name}/versions`),
  createPipeline: (pipeline: Partial<PipelineDefinition>) =>
    request<PipelineDefinition>('/pipelines', { method: 'POST', body: JSON.stringify(pipeline) }),
  updatePipeline: (name: string, pipeline: Partial<PipelineDefinition>) =>
    request<PipelineDefinition>(`/pipelines/${name}`, { method: 'PUT', body: JSON.stringify(pipeline) }),
  deletePipeline: (name: string) =>
    request<void>(`/pipelines/${name}`, { method: 'DELETE' }),
  createDraft: (name: string) =>
    request<PipelineDefinition>(`/pipelines/${name}/draft`, { method: 'POST' }),
  deploy: (name: string, version: number) =>
    request<PipelineDefinition>(`/pipelines/${name}/versions/${version}/deploy`, { method: 'POST' }),
  pause: (name: string) =>
    request<PipelineDefinition>(`/pipelines/${name}/pause`, { method: 'POST' }),
  resume: (name: string) =>
    request<PipelineDefinition>(`/pipelines/${name}/resume`, { method: 'POST' }),
  testMapping: (name: string, samplePayload: object) =>
    request<object>(`/pipelines/${name}/test`, { method: 'POST', body: JSON.stringify(samplePayload) }),

  // Topics
  listTopics: () => request<string[]>('/topics'),
  discoverSchema: (topic: string) => request<SchemaMap>(`/topics/${topic}/schema`),
  getSampleEvents: (topic: string, count = 5) =>
    request<object[]>(`/topics/${topic}/sample?count=${count}`),
}
