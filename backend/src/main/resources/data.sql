-- Optional sample learning content for local development.
-- This file contains no real user data.

INSERT INTO articles (title, content, category, source, word_count, view_count, created_at, updated_at) VALUES
(
  'How to Build a Clear Academic Paragraph',
  'A clear academic paragraph usually begins with a focused topic sentence, develops one main idea, and ends with a sentence that connects the point back to the larger argument. Strong paragraphs do not simply collect related sentences. They guide the reader through a line of reasoning. One useful pattern is claim, evidence, explanation, and transition. The claim states the paragraph focus. The evidence supports it with an example, quotation, or observation. The explanation shows why the evidence matters. The transition prepares the reader for the next idea. When writers revise their paragraphs with this structure in mind, their essays become easier to follow and more persuasive.',
  'Writing Skills',
  'InsightWrite Sample',
  116,
  0,
  NOW(), NOW()
),
(
  'Learning from Strong Sentences',
  'Close reading is not only about understanding meaning. It is also a way to absorb rhythm, structure, and expression. When you read a strong sentence, ask what makes it work. Does it use contrast, parallel structure, precise verbs, or a carefully placed modifier? Then try to imitate the pattern with your own topic. This does not mean copying the original idea. It means practicing the movement of the sentence. Over time, this kind of attentive imitation helps learners build a more natural sense of English style.',
  'Close Reading',
  'InsightWrite Sample',
  95,
  0,
  NOW(), NOW()
);

INSERT INTO knowledge_entries (category, title, content, tags, view_count, created_at, updated_at) VALUES
(
  'Grammar',
  'Using Parallel Structure',
  'Parallel structure means using the same grammatical form for related ideas. For example, "The course teaches students to plan, draft, and revise essays" is smoother than "The course teaches students to plan, drafting, and revision." Parallel structure improves clarity because readers can recognize the relationship between ideas quickly. It is especially useful in lists, comparisons, and argumentative writing.',
  'grammar,style,parallel structure',
  0,
  NOW(), NOW()
),
(
  'Revision',
  'From Correction to Upgrade',
  'A correction fixes an error. An upgrade improves the quality of expression. When revising English writing, start by checking grammar and word choice, but do not stop there. Ask whether each sentence supports the main idea, whether transitions are clear, and whether the tone matches the purpose. This is the difference between making a text acceptable and making it stronger.',
  'revision,writing improvement,clarity',
  0,
  NOW(), NOW()
);
