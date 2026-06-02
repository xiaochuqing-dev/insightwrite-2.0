import http from '@/api/index.js'
import { getStoredAnalysisPreferences } from '@/utils/preferences.js'

export function submitAnalyze(
  essayText,
  mode,
  customRequirement = '',
  topic = '',
  generateEssay = true,
  analysisPreferences = getStoredAnalysisPreferences()
) {
  return http.post('/analyze', {
    essay_text: essayText,
    mode,
    custom_requirement: customRequirement,
    topic,
    generate_essay: generateEssay,
    analysis_preferences: analysisPreferences
  })
}

export function getTaskStatus(taskId) {
  return http.get(`/analyze/${taskId}/status`)
}
