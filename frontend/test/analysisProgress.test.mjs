import assert from 'node:assert/strict'
import {
  countEnglishWords,
  getProcessingDetail,
  getProcessingPhases
} from '../src/utils/analysisProgress.js'

const sample = 'Artificial intelligence helps students revise essays with clearer structure.'

assert.equal(countEnglishWords(''), 0)
assert.equal(countEnglishWords(sample), 9)

assert.deepEqual(getProcessingPhases('deep'), [
  '拆解篇章结构',
  '提炼高分表达',
  '分析长难句',
  '整理可模仿写法'
])

assert.match(
  getProcessingDetail({ mode: 'deep', wordCount: 9 }),
  /约 9 词的佳作精读/
)

assert.match(
  getProcessingDetail({ mode: 'basic', wordCount: 9 }),
  /原文标注和分析结果/
)
