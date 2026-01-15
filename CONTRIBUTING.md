# Contributing to CivicLedger

Thank you for your interest in contributing to CivicLedger. This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Guidelines](#development-guidelines)
- [Pull Request Process](#pull-request-process)
- [Style Guides](#style-guides)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment (see [README.md](README.md))
4. Create a new branch for your work

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs. actual behavior
- **Environment details** (OS, Java version, Node version, etc.)
- **Screenshots** if applicable
- **Error logs** if available

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title** describing the suggestion
- **Detailed description** of the proposed functionality
- **Rationale** explaining why this would be useful
- **Possible implementation** approach (optional)

### Pull Requests

1. Ensure your code follows the project style guides
2. Include appropriate tests
3. Update documentation as needed
4. Reference related issues in your PR description

## Development Guidelines

### Security Requirements

All contributions must maintain security standards:

- No hardcoded credentials or secrets
- Input validation for all user inputs
- Parameterized queries for database operations
- Proper error handling without information disclosure
- Audit logging for security-relevant operations

### Accessibility Requirements

Frontend contributions must meet Section 508 / WCAG 2.1 AA standards:

- Proper semantic HTML
- ARIA labels where appropriate
- Keyboard navigation support
- Sufficient color contrast (4.5:1 minimum)
- Screen reader compatibility

### Testing Requirements

- Backend: Unit tests for services and integration tests for endpoints
- Frontend: Component tests where applicable
- All tests must pass before merging

## Pull Request Process

1. **Create a feature branch** from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following the style guides

3. **Test your changes**
   ```bash
   # Backend tests
   mvn test

   # Frontend linting
   cd frontend && npm run lint
   ```

4. **Commit your changes** with clear commit messages
   ```bash
   git commit -m "feat: add document search functionality"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Open a Pull Request** against the `main` branch

7. **Address review feedback** promptly

### Commit Message Format

Follow conventional commits format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types:
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `style` - Code style changes (formatting, etc.)
- `refactor` - Code refactoring
- `test` - Adding or updating tests
- `chore` - Maintenance tasks

## Style Guides

### Java Style Guide

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Document public APIs with Javadoc
- Use Lombok annotations where appropriate

### TypeScript/React Style Guide

- Use functional components with hooks
- Prefer TypeScript strict mode
- Use meaningful component and prop names
- Follow React best practices for state management
- Ensure accessibility in all UI components

### SQL Style Guide

- Use uppercase for SQL keywords
- Use snake_case for table and column names
- Include appropriate indexes
- Document complex queries

### Git Style Guide

- Keep commits atomic and focused
- Write clear, descriptive commit messages
- Rebase feature branches before merging
- Squash WIP commits before PR review

## Questions?

If you have questions about contributing, please open a discussion or contact the maintainers.

Thank you for contributing to CivicLedger!
