# Frontend Directory Reorganization

Reorganizing the frontend directory structure to match [versa-app](https://github.com/dig1t/versa-app/tree/main/app) pattern.

## Target Structure

```
src/
├── app/                    # Next.js routes (unchanged)
├── assets/
│   └── styles/
│       └── globals.css     # Move from app/
├── components/             # Flat structure - all UI components here
│   ├── button.tsx
│   ├── input.tsx
│   ├── form-field.tsx
│   ├── toast.tsx
│   ├── header.tsx
│   ├── sidebar.tsx
│   └── index.ts            # Barrel export
├── constants/              # App constants
├── features/               # Feature modules
│   ├── audit/
│   │   ├── audit-log-stream.tsx
│   │   └── index.ts
│   └── documents/
│       ├── document-list.tsx
│       ├── file-upload.tsx
│       └── index.ts
└── util/                   # Utilities (renamed from lib/)
    ├── api.ts
    ├── auth.tsx
    ├── auth-events.ts
    └── utils.ts
```

## Progress

### Phase 1: Create Directory Structure
- [x] Create `assets/styles/`
- [x] Create `constants/`
- [x] Create `features/audit/`
- [x] Create `features/documents/`
- [x] Create `util/`

### Phase 2: Move Files
- [x] Move `app/globals.css` to `assets/styles/globals.css`
- [x] Move `lib/*` to `util/`
- [x] Move `components/ui/*` to `components/` (flatten)
- [x] Move `components/layout/*` to `components/` (flatten)
- [x] Move `components/audit/*` to `features/audit/`
- [x] Move `components/documents/*` to `features/documents/`

### Phase 3: Update Imports
- [x] Update `app/layout.tsx` - globals.css import
- [x] Update `app/providers.tsx` - auth, toast imports
- [x] Update `app/login/page.tsx` - auth, button, form-field imports
- [x] Update `app/(dashboard)/layout.tsx` - utils, auth imports
- [x] Update `app/(dashboard)/dashboard/page.tsx` - auth, api imports
- [x] Update `app/(dashboard)/dashboard/documents/page.tsx` - auth, api, features imports
- [x] Update `app/(dashboard)/dashboard/upload/page.tsx` - auth, api, features imports
- [x] Update `app/(dashboard)/dashboard/audit-logs/page.tsx` - auth, api, features imports
- [x] Update `util/auth.tsx` - toast import
- [x] Update `components/button.tsx` - utils import
- [x] Update `components/input.tsx` - utils import
- [x] Update `components/form-field.tsx` - utils import
- [x] Update `components/toast.tsx` - utils import
- [x] Update `components/sidebar.tsx` - utils import
- [x] Update `features/audit/audit-log-stream.tsx` - utils, components imports
- [x] Update `features/documents/document-list.tsx` - utils, components imports
- [x] Update `features/documents/file-upload.tsx` - utils, components imports

### Phase 4: Create Barrel Exports
- [x] Create `components/index.ts`
- [x] Update `features/audit/index.ts`
- [x] Update `features/documents/index.ts`

### Phase 5: Cleanup
- [x] Delete old `lib/` directory
- [x] Delete old `components/ui/` directory
- [x] Delete old `components/layout/` directory
- [x] Delete old `components/audit/` directory
- [x] Delete old `components/documents/` directory
- [x] Delete old `app/globals.css` (after move)

### Phase 6: Verify
- [x] Run `npm run build` - ensure no errors
- [x] Run `npm run lint` - ensure no lint errors

## Completed: 2026-01-13

All tasks completed successfully. Build passed with no errors.
