# Object-Oriented Design Rule

This Pharmacy Management System is a case study in applying Object-Oriented Design (OOD) to a real backend system.

The goal is not to make the codebase huge or over-engineered. The goal is to keep it:
- small enough to understand
- structured enough to scale
- rich enough to show clean design decisions

## Core rule

Every feature in this project should be modeled using OOD principles first, not controller-first or database-first shortcuts.

That means:
- classes should have a single responsibility
- domain objects should own business meaning
- services should orchestrate workflows, not contain unrelated logic
- repositories should only handle persistence
- controllers should stay thin and transport-focused
- entities should reflect the real domain, not random CRUD tables

## What this project is demonstrating

This project is meant to show how a pharmacy system can be designed as a clean object model:
- procurement as its own bounded workflow
- inventory as batch-based domain logic
- sales as a separate workflow
- audit and traceability as first-class concerns

The system should feel like a real production backend, but still stay readable as a case study.

## Practical boundaries

Keep the design:
- not too big: avoid unnecessary abstractions, generic frameworks, and over-engineering
- not too small: do not collapse everything into one service or one giant entity

Prefer a balanced design where:
- each object has a clear purpose
- each workflow is traceable end to end
- the code reveals the business process

## Writing and implementation rules

1. Model the domain clearly before adding API code.
2. Prefer meaningful objects over primitive-heavy logic.
3. Put behavior where it belongs.
4. Use services for orchestration, not for dumping all logic.
5. Keep persistence models aligned with the schema, but let the domain stay readable.
6. Make the code easy to explain as a case study.

## Case study intent

When documenting or presenting this project, describe it as:
- a backend system built with Object-Oriented Design
- a real-world workflow system with clear domain boundaries
- a compact but complete example of thoughtful design

The final result should feel like a polished engineering case study, not a toy CRUD demo.
