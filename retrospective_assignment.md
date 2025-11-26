# Sprint 3 Review & Retrospective

## Sprint Review

**Sprint Goal.** Deliver a demo-ready SpendWise dashboard with actionable analytics, complete the Savings Circles feature set, and support the implementation with updated design documentation and unit tests.

**Objectives vs. Outcomes.** We committed in the pre-sprint agreement to: (1) embed MPAndroidChart visualizations showing category spend and budget usage, (2) finish the Savings Circles invitation and progress workflow backed by Firestore, (3) refresh the design packet (DCD, SD, SOLID, pattern evidence), and (4) add unit coverage for analytics aggregation and group rollover logic. All four objectives shipped. The dashboard now renders both pie and bar charts sourced from LiveData exposed by `DashboardAnalyticsViewModel`. The Savings Circles flow supports sending, accepting, and declining invitations, and the `SavingCircleViewModel` keeps member balances and cycle history synchronized. Design artifacts were merged into the shared drive and linked in the sprint deliverable. Two new unit test suites (`AnalyticsRepositoryTest`, `MemberCycleTest`) run green locally, covering data aggregation and rollover math.

**Adherence to Pre-Sprint Agreement.** Work stayed aligned with the ownership we established. Analytics tasks (chart integration, seeded fallbacks, view model plumbing) were handled by the dashboard sub-team; Savings Circles (invitation storage, lifecycle cleanup, UI polish) stayed with the social features pair; testing and design documentation were split across the remaining members. The only deviation was shifting UI polish for invitation empty-states to mid-sprint after Firestore schema tweaks delayed backend work; we agreed on the change during the Wednesday stand-up and absorbed the delay without impacting the demo scope.

**Completed Deliverables & Value.** The dashboard now provides real-time visibility into spending habits, enabling users to adjust budgets proactively. Savings Circles unlock collaborative savings challenges with clear progress tracking, differentiating SpendWise from standard budgeting apps. Updated design artifacts keep the documentation synchronized with the codebase, and the new unit tests give us automated regression checks around the most logic-heavy features.

## Sprint Retrospective

**Team Dynamics.** Communication was generally strong—Slack updates remained steady, and daily stand-ups surfaced blockers quickly. Pair programming between the analytics and repository owners shortened integration time. We did encounter some friction around coordinating Firestore rules changes; clarifying ownership during mid-sprint planning mitigated confusion.

**What Worked Well.** Early alignment on data schemas let us stub repositories and unblock UI work. The strategy/factory abstractions around chart configuration made it easy to experiment with additional visualizations. Sharing test utilities increased coverage without duplicating fixtures. Hosting mid-sprint demos fostered shared understanding of progress.

**Areas for Improvement.** We underestimated the time required to refactor existing ViewModels to reuse analytics helpers, leading to a crunch late in the sprint. Design artifact updates were started near the deadline, creating parallel work that could have been spread earlier. Finally, onboarding new contributors to Firestore emulators needs a clearer guide; local setup problems slowed testing for a day.

**Actionable Steps.**
- Front-load design/documentation tasks by scheduling a dedicated working session in the first half of Sprint 4.
- Extend the onboarding README with explicit Firestore emulator setup instructions and sample data seeding scripts.
- Add setup scripts to automate provisioning of seeded analytics data so new test cases can be written quickly.
- Reserve a mid-sprint hour for refactoring debt, ensuring shared utilities are stabilized before UI integration picks them up.

**Overall Sentiment.** The team delivered the agreed scope, tightened collaboration across feature boundaries, and produced customer-visible improvements. With earlier documentation updates and better tooling around environment setup, we expect to move faster in Sprint 4.