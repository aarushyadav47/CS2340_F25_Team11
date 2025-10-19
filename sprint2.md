
8
CS 2340 Sprint 2
Budget Goals, Expense Logging & Firebase
Due Date: See Canvas assignment
Background
This semester’s project is SpendWise: A Personal Finance & Budgeting App, designed to help users plan
budgets and track day-to-day spending with a clean, student-friendly workflow.
Tasks
For Sprint 2, you are asked to create and submit multiple design deliverables to Canvas along with
demoing the functionality implemented. For the design deliverables, you will perform object-oriented
analysis (OOA), to refine the Domain model you created in Sprint 1. You will develop a Sequence Diagram
(SD) and submit evidence of how the team implemented the Singleton Design Pattern. For the
implementation portion of this sprint, you will begin implementing the core budgeting and expense-
tracking features of SpendWise—including budget creation/validation (weekly or monthly), expense
logging by category with a date-sorted list, and a Singleton database instance under a Java/MVVM
architecture. Other than the requirements outlined below and that you must use Java (if you are still using
Kotlin, that’s concerning), the details of your implementation are up to you. Your application
implementation and functionality will be graded during a demo, which will occur the week after sprints
are due; see the class schedule for specific dates.
Pre-Sprint Agreement
Create an itemized list of tasks to be completed by the team and assign them to individuals. This list should
be submitted as a document to the Pre-Sprint Agreement Canvas assignment for your section. This list
can contain additional requirements such as a completion date for the task or subtasks, if you so wish.
You should also attach a screenshot of the project management tool you are using to assign said tasks to
team members, making sure that it is accurate to an Agile workflow.
The most important element to this assignment is agreeing as a team to the work to be done by everyone.
We will only be checking this assignment for completion but may refer to it in the future if there are any
issues with team members not pulling their weight. Ensure that the work distribution is fair and agreed
upon by everyone on the team. Please refer to Canvas for the due date of this assignment.
Design Deliverables
Three design deliverables are required in this sprint: a revised Domain Model, a Sequence Diagram, and
Design Pattern Evidence. These are due as a combined single PDF via Canvas submission (check course
page for official date). For diagramming, we recommend require using draw.io.
Revised Domain Model
1. Update the list of nouns from your previous domain model to include the features from this sprint.
You must include at least two new nouns. Highlight the new nouns in some way to distinguish them
from Sprint 1 nouns.
2. Update classes and attributes with your new list of nouns and list the identified classes and attributes
on your submission PDF.
3. Revise your Domain model with the new added classes and attributes.
4. Add the new classes to your Domain model and connect them with at least one other class using
associations
a. Include multiplicities for each association, one on each side of the association.
Please explicitly categorize any new nouns as either classes or attributes somewhere in your deliverable
in addition to including in the domain model. The submission of the Domain Model itself does not suffice
for the inclusion of the listed and categorized nouns.
Sequence Diagram
1. Create a Sequence Diagram (SD) outlining a use case from the last sprint from the Use Case diagram
a. Ensure the SD illustrates the interaction between at least 3 classes/modules within your
program. It may be useful to consider your implementation of MVVM.
b. The SD must contain at least one ALT, LOOP, or OPT structure.
c. Hint: don’t forget activation bars!!!
Singleton Evidence
1. Submit screenshots of your code abiding by the Singleton pattern. For example, you may consider
implementing the class that manages database connections as a Singleton class.
2. The screen shots should capture a clear view of how the singleton design pattern was used.
3. Write a paragraph description explaining which class implements the Singleton design pattern
and justify your reasoning.
Implementation
1. In this sprint, you will begin implementing the Budgeting & Expense functionality of your finance app.
This sprint focuses on expense logging and budget goals/windows.Expense Logging (Expense Log
screen)
a. Provide a “+” button on the Expense Log screen that opens an Expense Creation form.
b. The form must allow users to input: expense name, amount, category (must be chosen from
categories already defined on the Budgets screen; include an “Other” option), date (current
or past), and notes (optional).
i. Include validation for required fields, amount > 0, and date ≤ current date.
c. Store created expenses in a per-user Firestore collection (e.g., users/{uid}/expenses).
d. Ensure that each user (or new account) has a few pre-populated expense entries the first time
they access the app. This will help verify Firestore integration and UI rendering even before
new expenses are created manually.
e. Display expenses on the Expense Log screen in a scrollable list, showing at minimum the
name, amount, category, and date.
f. The list must be sorted by expense date (descending) and populated from Firestore; seed
the database with at least 2 example expenses.
g. Data access should be mediated through a Singleton instance.
2. Budget Goals & Budget Window Calculator (Budgets screen)
a. Provide a Budget Creation form with fields: title/name, total budget amount, category (e.g.,
Food, Transportation, Entertainment), frequency (weekly or monthly), and start date
(weekly: first day of the 7-day window; monthly: first day of the month).
b. Store budgets in a per-user Firestore collection (e.g., users/{uid}/budgets).
c. Seed the database with at least one or two example budgets for new or first-time users. These
sample entries can help test UI behavior, validation, and budget-expense linkage functionality.
d. Implement validation: required fields, non-negative amounts, and valid start dates
(consistent with chosen frequency).
e. Display all budgets in a scrollable list:
i. Sort by creation date (newest first).
ii. Provide status indicators: green → completed, yellow → in progress, red →
incomplete.
f. Tapping a budget opens Budget Details showing:
i. All budget fields (title, amount, category, frequency, start date).
ii. A visual progress indicator and any surplus if the budget finishes under target.
g. Budget Window Calculator (2-of-3): Provide three inputs—Total Budget, Spent-to-Date,
Remaining. If any two are filled, a button computes the third. Save the computed values to
the selected budget.
h. Ensure expenses and budgets are linked (e.g., by category or an optional budgetRef), so
utilization can be computed per budget.
3. Totals & Dashboard
a. Display aggregate totals (e.g., total spent this period and remaining by category/budget)
derived from the expenses collection and current budget windows.
b. Respect the app’s current date (testing selector) when computing weekly/monthly rollovers.
4. Calendar Selector and Logout Button
a. Add a small calendar icon component to the Dashboard screen that allows users to select and
set a custom “current date.”
b. This simulated “current date” will apply across the app for: expense logging (for testing date-
based expense functionality) and budget tracking (weekly/monthly rollovers).
c. This feature is for developer testing and will serve as the foundation for time-sensitive logic.
During the sprint, you will be demoing this feature, so be sure to pre-populate your Firebase
with some relevant information to show the functionality.
d. Add a logout option accessible from the dashboard. This should take you back to the login
screen and clear any cached data in the application.
Note: You should pre-populate your Firebase database with a small set of example budgets and expenses
(e.g., 2–3 each) when a new user account is created or when the app is launched for the first time. These
examples should cover different categories and time periods to help verify app functionality across various
features (Expense Log, Budgets, and Dashboard).
Make sure any rollover functionality introduced in Sprint 1 is fully implemented and working, since
incomplete or incorrect rollovers can lead to issues in upcoming sprints.
As in previous sprints, you have freedom over visual design. You may use any suitable charting library.
Additional app functionality is welcome as long as the above requirements are met and you maintain Java
+ MVVM with Firestore access routed through ViewModels/Repository.
Testing Requirement
Starting this sprint, your team will be required to create 2 unit tests per team member. Make sure you
create and develop your project with unit testing in mind. It may be wise to write some unit tests for the
implementation requirements to practice for future sprints. For Sprint 2 only, you are allowed to construct
unit tests that test functionalities from Sprint 1 and/or Sprint 2. Design and implement with testability in
mind (small, single-responsibility classes; logic in ViewModels/Repository; no Firestore calls from Views).
Some example unit tests that you could prepare from Sprint 2 requirements are:
1. Given expenses linked to a budget/category, computed percent utilized and surplus are correct.
2. Reject expense dates after the app’s current date; accept current/past dates.
Similarly, example unit tests from Sprint 1 are:
1. Detection of whitespace-only, null, and empty inputs for account login and creation
2. Preventing invalid accounts from logging into the application (accounts not stored in Firebase).
Note: The UI of your team’s application should be separate from the game’s functionality/logic. These
tests should not be composed of UI mocking, but instead, your team’s tests should be validating your
app’s logic and event handling. If you create mock test cases, you will receive 0 credit for those tests.
You may want to reference the Android Studio Assignment template we provide for Sprint 0.5. JUnit 4 has
been added to the dependencies in the Android-Gradle for that repository. To write a unit test, navigate
to the subpackage marked as “(test)” in Android Studio. Annotate each unit test with “@Test” as shown:
Unit tests can be run individually or altogether using the green play button near each test method/class:
For further knowledge on Android testing, refer to the documentation.
Code Review
At the end of this sprint, your team will also be expected to complete the Code Review assignment. This
should not be any different than the Code Review submission for Sprint 1. Please ensure that you read
the directions for the Sprint 2 Code Review assignment carefully so that you don't run into any issues with
the grading.
Important: For pull requests created, the autograder will only count pull requests with at least 15 changes
(lines added + deleted) and at most 300 changes as valid PRs. Pull requests with fewer than 15 changes or
more than 300 changes will be marked as invalid and will not contribute to your score.
The quality of your pull requests is very important when you are a software engineer in industry, having
clear code and being able to explain why you made such changes with clear PR comments, commit
messages, and reviewing PRs properly. We will be reading these, and they need to show that you have a
proper understanding of what is being added to the codebase. This article goes into a little more detail.
CheckStyle
CheckStyle is required for this sprint. You can refer to the details mentioned in Sprint 1 for the set up.
Make sure you follow the rules for the correct styling of your project’s code.
Sprint Tagging
Tags are a way of marking a specific commit and are typically used to mark new versions of software. To
do this, use “git tag” to list tags and “git tag –a tag name –m description” to create a tag on the current
commit. Important: To push tags, use "git push origin –tags". Pushing changes normally will not push
tags to GitHub. You will be asked to checkout this tagged commit during demo. This is done with “git fetch
--all --tags” and “git checkout tag name”. You will be required to pull this tag during the demo. If you
forget to tag your commit or tag your commit late, you will be penalized with –5 points for each day
late.
Additional Resources
Under this section are some resources we think will help you and your team streamline the game creation
process. If you have any questions about these resources, contact your TAs through Ed or office hours.
1. TA website: https://github.gatech.edu/pages/gtobdes/obdes/
2. Ed: https://edstem.org/us/courses/81858/discussion/7065024
3. Android Studio Tutorial Videos (located on the canvas lecture page).
Demos
Your implementation of sprint features will be graded through an in-person TA demo (Each team will
demo with their corresponding mentors). Demos will be held in CCB 267 the week after the sprint is due.
Please sign up for a demo slot in Microsoft Bookings with your assigned mentor if possible. Note that not
all demo slots may be open at sprint release.
Summary
You will be graded according to the successful and correct completion of the design deliverables and
implementation requirements above. Groups are required to demo to receive credit for the features they
have implemented. This will be done during TA office hours after the due date of the design deliverables.
TA demos will be able to be booked through Microsoft Bookings. You will submit your design deliverables
as a combined PDF via the Canvas assignment. You should also include a link to your GitHub project
repository. Your repository must be set to private. When you sign up for a demo, ensure that the shared
TA GitHub account has been added to your repository and you have tagged the code you intend to demo.
Points may be deducted if these guidelines are not followed.
Academic Integrity
Note on Plagiarism and the Use of AI Tools
We understand that technology and tools, including Artificial Intelligence platforms, have become readily
accessible and can be valuable in various scenarios. However, students must be cautious about their usage
in academic settings. Please refer to the course policy regarding the use of AI tools for your projects. Using
AI to generate or copy content for your project is not considered collaboration. Leveraging AI to produce
work that you present as your own, without proper citation, is likely crossing the line into academic
misconduct. It's essential to maintain integrity in all academic undertakings. If in doubt, always consult
the course guidelines or reach out to the instructor on Ed Discussion for clarification. If you are violating
academic integrity policies, you WILL be caught and reported to OSI, which could result in serious
consequences.