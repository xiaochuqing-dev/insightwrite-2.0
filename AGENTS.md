# InsightWrite 2.0 Project Rules

These rules apply only to this personal project. Do not repeat global defaults here unless the project needs a narrower rule.

## Product

- InsightWrite 2.0 is a personal full-stack app for English writing analysis, revision suggestions, learning content, favorites, history, credits, authentication, and profile workflows.
- Do not reposition the product by renaming the project; use copy, navigation, prompts, and entry-page wording instead.
- In user-facing Chinese product copy, prefer `英语` over `英文` when describing the product or brand experience.

## Change Boundaries

- Keep changes aligned with the existing Vue/Vite frontend and Spring Boot backend structure.
- Do not add production dependencies unless the task clearly requires them.
- Do not expose real secrets, passwords, tokens, mail authorization codes, database credentials, or API keys in replies, docs, commits, or unrelated files.
- Before changing project content, check whether the repository already has a Git commit. If it has none, create one first. After completing the requested change, create a Git commit for the result.

## Frontend

- Frontend code lives in `frontend/` and uses Vue 3, Vue Router 4, Vite, and Axios.
- Use the Axios instance in `frontend/src/api/index.js` for API calls.
- Keep routes in `frontend/src/router/index.js`, views in `frontend/src/views/`, reusable components in `frontend/src/components/`, and display helpers in `frontend/src/utils/`.
- Preserve the existing `localStorage` token flow and request interceptors unless the task is about authentication.
- Verify frontend changes with `cd frontend && npm.cmd run build`.

## Backend

- Backend code lives in `backend/` and uses Spring Boot 3.4, Java 17, Spring MVC, JPA, and MySQL.
- Keep the existing Controller, Service, Repository, DTO, and Entity layering.
- Preserve existing `/api` routing, auth interception, and response-shape conventions.
- Verify backend changes with `C:\Users\Administrator\Desktop\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`.

## Verification

- If both frontend and backend change, run both verification commands when practical.
- For auth, credits, password reset, email code, AI-cost, or database changes, add a targeted smoke check when practical.
