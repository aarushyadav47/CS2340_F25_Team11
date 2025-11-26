CS 2340 Sprint 4
AI Chatbot Integration, In-App Reminders, Extra Credit Features
Due Date: See Canvas assignment
Background
This semester’s project is SpendWise: A Personal Finance & Budgeting App, designed to
help users plan budgets and track day-to-day spending with a clean, student-friendly
workflow.
Tasks
For Sprint 4, you are asked to finalize and enhance SpendWise by integrating AI functionality
and ensuring full application stability. You will incorporate an AI-powered Chatbot on the
Chatbot screen that connects to a free API to provide personalized budgeting tips, financial
FAQs, and motivational advice. You will also add in-app pop-up reminders to notify users
when they have not logged in any expenses since their last active session or when their
budgets are nearing their limits, ensuring they are consistent and error-free. In addition, you
can polish the user interface for visual consistency across all screens, standardize styles
and layouts, and verify real-time synchronization of budgets and expenses between
personal and group accounts through Database. Finally, you will administer thorough
testing and debugging to confirm smooth navigation and reliable performance across the
full app. You should still maintain MVVM architecture and clean, documented code. Extra
credit features can also be implemented and demonstrated in this sprint.
Other than the requirements outlined below and that you must use Java (if you are still using
Kotlin, that’s concerning :|), the details of your implementation are up to you. Your
application implementation and functionality will be graded based on a video submission
that not only showcases all the new functionality implemented for sprint 4 but also
functionalities across all sprints. The video should showcase all implemented
functionalities for each screen in this full app flow, Login → Dashboard → Expense Log →
Budgets → Group Savings → Chatbot → Logout.
Pre-Sprint Agreement
Create an itemized list of tasks to be completed by the team and assign them to individuals.
This list should be submitted as a document to the Pre-Sprint Agreement Canvas
assignment for your section. This list can contain additional requirements such as a
completion date for the task or subtasks, if you so wish. You should also attach a screenshot
of the project management tool you are using to assign said tasks to team members, making
sure that it is accurate to an Agile workflow.
The most important element to this assignment is agreeing as a team to the work to be done
by everyone. We will only be checking this assignment for completion but may refer to it in
the future if there are any issues with team members not pulling their weight. Ensure that
the work distribution is fair and agreed upon by everyone on the team. Please refer to
Canvas for the due date of this assignment.
Design Deliverables
Three design deliverables are required in this sprint: GRASP Principles Writeup, Design
Pattern Evidence, and Code Smells Fixes Evidence. These are due as a combined single
PDF via Canvas submission (check course page for official date).
Grasp Principles Write-Up
1. 2. Identify and document examples of GRASP principles applied within your
implementation.
a. Capture screenshots of relevant code and provide an explanation (3-4-
sentences) for each principle used.
b. The number of GRASP principles should correspond to the number of team
members divided by two, rounded up (e.g. if there are 5 team members,
provide 3 examples)
c. Each example should illustrate a different GRASP Principle (e.g. Controller,
Information, Expert, Low Coupling)
Modification of Previous Code (required if not implemented already):
a. You may revise previous Sprint implementations to showcase improved
compliance with GRASP principles, where relevant, if not implemented already.
Design Pattern Evidence
1. 2. 3. Submit screenshots of your code abiding by 2 design patterns of your choice (among the
design patterns we covered in class) for some implementation of the Sprint 4
requirements. This should be a design pattern that you have not already implemented.
The screenshots should capture a clear view of how the design patterns were used for
their implementation, including all relevant classes.
Write a paragraph for each design pattern describing how your code properly utilizes a
specific design pattern.
Code Smells Evidence
1) Submit screenshots of code smells in your whole project that was detected by
2) SonarQube
a) Must showcase the same number of code smells as group members
For each code smell, have screenshots showcasing your fixes for that code smell
along with a single paragraph explanation of what the code smell issue was and how
you fixed it.
Implementation
In Sprint 4, you will deliver two major enhancements: (1) an AI-Powered Chatbot
integrated on the Chatbot screen that provides personalized budgeting tips, and answers
financial FAQs using a free AI API, and (2) In-App Pop-Up Reminders that notify users if
they have not logged expenses since their last activity or if budgets are approaching their
set limits. Maintain Java + MVVM separation (no API or Database calls from Views), use
observable streams (e.g., LiveData/Listeners) for real-time updates, and write unit tests for
all new business logic.
*** The UI Mockups are there to help you showcase how your app can potentially look
like. It is not fully comprehensive, so make sure you read the document thoroughly.
***
A) AI Chatbot Integration
Implement an AI-powered Chatbot module on the Chatbot screen to provide personalized
budgeting guidance. The Chatbot must connect to a free AI API to return responses based
on user queries.
1) Required:
a. b. Integrate with a free AI API (e.g., Ollama) to generate responses.
The chat bot should be able to provide personalized budgeting tips and nswers to
financial FAQs.
c. Maintain MVVM separation
a. Any API calls and response handling must occur within ViewModels
d. Adequately handle any empty and error states gracefully (e.g., when the API is
unreachable or returns no response).
e. f. Support real-time updates of messages using LiveData or observable streams.
Expand the existing AI Chatbot into a more advanced AI Financial Advisor capable
of contextual and data-driven insights.
g. h. i. j. k. Store condensed conversation summaries in Database for contextual continuity
between sessions.
Introduce custom command handling, such as: “summarize my spending this
week”, “suggest where I can cut costs”, “how did I perform compared to last
month?”, etc. These prompts should use Database data (expenses, budgets, goals)
to compute insights before forwarding the query to the AI API for human-like
phrasing.
Generate a unique title for each new chat using the AI API based on the first user
prompt. Store this title and associated chat data in the Database for future
reference.
Enable referencing of previous conversations:
• Before sending a new message, prompt the user with a pop-up asking if they
want to include data from a previous chat.
• If “Yes,” display a list of previous chat titles (retrieved from the Database) for
selection.
• The selected chat’s summarized context should be combined with the current
query before sending it to the API.
• If “No,” proceed with the new prompt independently.
• Ensure all conversation summaries, titles, and reference relationships are
stored in the Database.
Ensure that all Database access and AI calls remain encapsulated within the
ViewModel layer, maintaining the MVVM architecture.
2) UI/UX Behavior for the Chatbot:
a) b) Implement a scrollable chat interface with user and bot message bubbles,
timestamps, and an input text bar.
Display placeholder text or friendly error messages when responses cannot be
fetched.
c) Store minimal message history per session for context in the database.
B) In-App Pop-Up Reminders
Add pop-up reminder functionality to help users stay consistent with expense logging and
mindful of their spending limits.
1. Missed Expense Log Detection
a. On login, the app should check the date of the user’s last recorded expense
and display a reminder indicating how many days it has been since their last
log (e.g., ‘It’s been 3 days since your last expense!’).”
i. If the user is up to date with logging expenses, then no popup should
appear
b. Pop-up should be able to lead you to the expense log page where you can log
in your missed expenses
c. Pop-ups must be dismissible, consistent, and non-intrusive.
i. Pop-ups shouldn’t be separate screens and should be a small
window that shows up whenever you’re logged in.
2. Budget Limit Warnings
a. Trigger a pop-up when user spending approaches a predefined threshold
(e.g., 80–90%) of the set budget.
b. Display clear progress information in the popups (e.g., “You’ve reached 85%
of your Entertainment budget”).
c. Implement a notification queue to ensure that only one pop-up appears at a
time, keeping the experience non-intrusive and preventing overlapping
alerts.
3. Validation & Design
a. b. Validate to prevent null values or repeated alerts.
Maintain an appropriate visual consistency with app theming and ensure
pop-ups display properly across devices
c. Ensure that the app can handle multiple pop-ups and that they don’t overlap
or interfere with one another
C) Final Integration, Testing, and Architecture
Refine all SpendWise features to ensure full functionality, consistent UI/UX, and stable
app performance across all screens.
1. End to End Integration and Testing
a. Verify the seamless flow of operations across all screens: Login → Dashboard
→ Expense Log → Budgets → Group Savings → Chatbot → Logout, ensuring that
data, updates, and user actions propagate correctly throughout the app.
i. You will demonstrate this requirement in your demo videos. To help
illustrate this, two scenarios are provided below that you will use to
showcase the app’s functionality and real-time updates.
b. Confirm real-time Database synchronization across budgets, expenses, and
group savings.
2. UI/UX Polish and Consistency
a. Apply consistent headers, spacing, fonts, and button styles.
b. Standardize chart colors and legends across dashboard visualizations.
c. Refine empty state messages to clearly guide new users.
d. Ensure scrollable layouts and responsiveness across different screen sizes.
3. Bugs and Error Fixing
a. Fix crashes, data mismatches, and incorrect chart displays.
b. Handle error cases such as invalid inputs, Database read/write failures, and
missing fields.
c. Ensure handling of network issues when fetching AI responses or syncing
with Database.
d. Complete any unimplemented or incomplete features from previous sprints
to ensure full end-to-end functionality before submission.
4. Architecture and Best Practices
a. Remove all unused code and comments from previous sprints.
b. Maintain Java + MVVM structure, encapsulating Database and API logic in
repository classes while Views only observe LiveData.
c. Include clear comments and documentation for all new or modified
methods.
2. 3. For end-to-end testing and evaluation, your demo video must walk through the two
provided testing scenarios below, covering every point discussed in them as well as all
core functionality implemented in your application. This walkthrough serves to
demonstrate complete, working app functionality. It is not the only submission
component, but it must clearly show that all required features are operational.
Scenario 1: Rahul’s Personal Finance Journey
1. Rahul opens SpendWise and registers a new account using email and password.
Upon first login, the Dashboard, Expense Log, Budgets, Savings Circles, and
Chatbot tabs display placeholder text and empty state visuals to guide him.
From the Dashboard, Rahul navigates to the Budgets screen and creates 3 budgets,
specifying title, category, total amount, frequency (weekly/monthly), and start date.
Each budget is stored in Database under Rahul’s user ID. Each budget Rahul
creates is stored in Database under his user ID, and all three budgets have titles
(unique), categories, amounts, frequencies, and start dates.
Rahul then navigates to the Expense Log screen and logs multiple expenses across
categories (Food, Transportation, Entertainment). He accidentally inputs a future
date for an expense, but luckily the app reminded him to put a valid date. He then
continues to log expenses, and in the end has a grand total of 3 expenses. Expenses
are written to Database and linked to their corresponding budgets.
Returning to the Dashboard, Rahul observes real-time visualizations.
a. Example: a pie chart showing expenses by category and a bar chart showing
budget usage (spent vs. target).
Rahul uses the calendar date selector on the Dashboard to view expenses for
different days. Charts and progress bars adjust immediately to reflect the selected
date.
this.
A pop-up reminder appears indicating that the budget is nearing its limit. He
dismisses this pop-up.
Rahul then navigates to the Chatbot tab and asks for personalized saving tips as he
just used up a lot of one of his budgets. Responses are displayed as user and AI
message bubbles with timestamps. The chatbot handles network errors gracefully
by displaying error messages without crashing.
4. 5. 6. He then decides to spend 90% of one of his budgets and logs in another expense for
7. 8. 
9. Rahul opens the Savings Circles screen and creates a new group savings challenge,
specifying group name, challenge title, goal amount, frequency, and inviting his
friend Vignesh. Database stores the group metadata under Rahul’s user ID.
10. Rahul logs a personal contribution toward the group goal through the Expense Log
screen. The group progress bar updates in real time on both Rahul and Vignesh’s
dashboards.
11. Rahul logs out using the Logout button on the Dashboard. The app returns to the
Start/Quit screen, clearing all cached and Singleton data to ensure a clean session
transition.
Scenario 2: Vignesh’s Collaborative & Personal Tracking
(For this scenario make sure Vignesh has an account with pre-existing budgets and
expenses that have been dated to at least 3 days prior)
1. 2. Ensure Vignesh’s account exists with pre-existing budgets and expenses.
Vignesh logs in using his credentials and dismisses and any pop-ups that indicate
his inactivity of logging his expenses.
3. 4. He navigates to the Dashboard that immediately displays real-time visualizations.
He then navigates to the Savings Circles tab. Vignesh sees a pending invitation from
Rahul. He accepts one group challenge and declines another. Database updates
his acceptance status immediately. The accepted group appears under “Active
Challenges,” while the declined one is removed.
5. Vignesh views accepted group details, including members, challenge start/end
dates, and current contributions. He cannot edit group challenge information but
can log contributions.
6. Vignesh logs an expense toward the accepted group goal via the Expense Log
screen. Database updates contributions for both Vignesh and Rahul in real time,
and progress bars on their dashboards reflect the new data.
7. Navigate to the Budgets screen. Vignesh creates a new weekly budget for
Transportation. Immediately after, he logs an expense that contributes toward this
budget. Both budget and expense are stored in Database and linked correctly.
8. Returning to the Dashboard, Vignesh observes updated visualizations reflecting all
expenses and budgets.
9. Vignesh opens the Chatbot tab and requests help. Responses are displayed in the
scrollable chat interface with timestamps.
10. Log out from Vignesh’s account using the Logout button. The app navigates back to
Start/Quit, clearing all cached and Singleton data.
a. b. c. d. e. 11. Log back into Rahul’s account. Navigate to the Savings Circles tab. Rahul observes
that Vignesh has accepted one of the group challenges; his name appears in the
member list with contributions accurately reflected. At no point should the app be
restarted during this demonstration.
12. Throughout this scenario, all Database reads/writes, dashboard analytics, budget
warnings, group contributions, streak counters, and AI chatbot interactions should
update in real time, validating end-to-end Sprint 4 functionality.
Extra Credit [30 points]
Features [15 points]
1. Dark Mode Toggle [5 points]
Allow users to toggle between Light and Dark modes within the application.
The app should default to Light Mode for first-time users.
The selected mode should persist across sessions using SharedPreferences for
local caching.
When a user logs in, their last chosen theme preference should be fetched from
Database and applied automatically.
On logout, the local theme state should reset, ensuring that each user’s mode
preference is independent and stored per-user in Database.
UI elements, charts, and pop-ups should dynamically adapt to the selected
theme without requiring an app restart.
2. User Profile [10 points]
a. Create a User Profile section within the app that allows users to view and
manage their personal information (his can be implemented as a separate tab or
a Profile button/icon)
The Profile screen must display key user information retrieved from the
Database, including email address used during registration and basic user
statistics (e.g., total expenses logged, total budgets created, or other app-
relevant data).
Implement an option to upload and display a profile picture:
• The picture should be stored in the Database (e.g., Firebase Storage) and
linked to the user’s account.
• Ensure that the uploaded image persists across sessions and reloads
correctly on login.
• Include a placeholder avatar if the user has not uploaded a picture yet.
Add a Friends system to encourage social engagement:
• Allow users to search for and add friends by username or email.
• Display the number of friends the user has and a list of added friends (names
or profile pictures).
f. a. b. c. d. 
e. • Include a simple option to remove or unfriend users if needed.
• Each friend connection should be mutual and stored in the Database under
both users’ profiles.
The User Profile section should be visually cohesive with the rest of the app:
• Use consistent color themes (respecting Dark/Light Mode if implemented).
Individual component [15 points]
Website Deployment on GitHub Pages [5 points]
An additional challenge for a chance to earn up to 5 extra credit points by creating a detailed
showcase website for your "SpendWise" project on GitHub Pages. This platform will allow
you to exhibit the intricate details and functionalities of your application, reflecting the effort
and innovation embedded within. This is also a great way to include this project as part of
your resume. Ensure your website comprises the following sections:
• Introduction: Have a comprehensive introduction to the project, emphasizing its aim to
ease the process of creating and managing travel itineraries for solo and group travel.
• Design & Architecture: Elaborate on the architectural design and the implementation of
design patterns that underpin your project. Include relevant UML diagrams, such as
Design Class Diagrams, to provide a clear visual representation of your application's
design.
• User Interface (UI): Showcase a guided visual tour of the application using annotated
screenshots. Highlight key screens such as Dashboard, Expense Log, Budgets, Savings
Circles, and the AI Chatbot. Explain how your team approached UI/UX consistency,
theming, and user navigation.
• Functionality: Include a link to a video demonstration that captures your app's
functionality, ensuring a direct insight into its operational features and user interface.
• Conclusions and Reflections/Learning: Wrap up with a reflective overview of the project
outcomes, detailing your contributions, the challenges encountered, and the knowledge
gained throughout the development process.
• Contributors: Acknowledge all team members who contributed to the app's
development. Specifically, highlight those who were instrumental in the website's
deployment
(Note: Only those directly involved in creating the website qualify for the extra credit points
for this part.)
LinkedIn Learning article [7 points]
For this part you will write a LinkedIn Learning–style article reflecting on a concept, skill, or
design principle they learned while developing SpendWise. The goal is to share meaningful
technical or design insights gained through real project experience.
Your article should include the following elements:
• Choose a concept, pattern, or skill you developed or strengthened while building
SpendWise.
• Reflect on how you encountered this concept in the project. Explain briefly how you
applied it in your implementation, what challenge it solved or what value it added to
your team’s work.
• Write the article as if you’re explaining the concept to another student about to take
this course. Use examples or screenshots (if helpful), explain the “why” behind your
approach, and discuss key takeaways or lessons learned. Keep your tone clear,
authentic, and educational. Avoid overly formal or AI-generated phrasing.
• Reference your SpendWise app throughout the article and, if applicable, include a
link to your GitHub Pages website. This helps contextualize your learning and
showcase your work.
• Publish your article publicly on LinkedIn under your own account. Title it clearly and
include any images, diagrams, or short code snippets that help explain your concept.
Note: The article must reflect your own writing and understanding (not AI-generated
content). Submissions showing signs of AI-generated text may receive no credit for this
section.
Individual LinkedIn Showcase [3 points]
Complement your website deployment and the LinkedIn Learning article with a personal
LinkedIn post. This post should narrate your project journey, spotlight your individual
contributions, and what you learned during the project's lifecycle. Include a link to the
GitHub Pages website and the link to video demo if you want to support your narrative.
Tagging your team members, Dr. Roy, your mentor, or any TAs is encouraged to foster
engagement and visibility. If multiple group members want to participate in this opportunity
together, then one member can create the post and others can share the post, adding a few
lines of their own.
Submission for extra credit (individual component)
Create comments on the sprint 4 group submission as: Name of team member, GitHub
pages link and LinkedIn post link. Create separate comments for separate team members
who participate in the extra credit.
Testing Requirements
Your team will be required to create 2 unit tests per team member. Make sure you develop
the project with unit testing in mind, as it will help ensure robustness and catch issues early.
Unit tests should focus on the functionalities introduced in Sprint 4. For this Sprint, testing
should cover the new AI Chatbot Integration or Popups integration.
Provide 2 unit tests per team member targeting Sprint 4 functionality. Design for
testability (singleresponsibility classes; logic in ViewModels/Repository). Examples:
• AI Chatbot Integration: validate proper parsing of AI responses, display in chat
interface, and graceful handling of network errors or empty responses.
• Pop-Up Reminders: verify that reminders trigger when budgets are nearing their limits
or when missed expense logs are detected; ensure dismissible behavior and non-
persistence after acknowledgment.
For instructions on setting up JUnit 4 and running test cases, please refer to the Sprint 2
document.
For further knowledge on Android testing, refer to the documentation.
Note: The UI of your team's game should be separate from the game's functionality/logic.
These tests should not be composed of UI mocking, but instead, your team's tests should
be validating your game's logic and event handling. If you create mock test cases, you will
receive 0 credit for those tests.
Code Review
At the end of this sprint, your team will also be expected to complete the Code Review
assignment. This should not be any different than the Code Review submission for Sprint 1.
Please ensure that you read the directions for the Sprint 4 Code Review assignment carefully
so that you don't run into any issues with the grading.
Important: For pull requests created, the autograder will only count pull requests with at
least 15 changes (lines added + deleted) and at most 300 changes as valid PRs. Pull requests
with fewer than 15 changes or more than 300 changes will be marked as invalid and will not
contribute to your score.
The quality of your pull requests is very important when you are a software engineer in
industry, having clear code and being able to explain why you made such changes with clear
PR comments, commit messages, and reviewing PRs properly. We will be reading these, and
they need to show that you have a proper understanding of what is being added to the
codebase. This article goes into a little more detail.
CheckStyle
CheckStyle compliance is required for this Sprint. Refer to the setup details provided in
Sprint 1 to ensure proper code styling. Make sure your project code follows all specified style
guidelines.
Sprint Tagging
Tags are a way of marking a specific commit and are typically used to mark new versions of
software. To do this, use "git tag" to list tags and "git tag –a tag name –m description" to
create a tag on the current commit. Important: To push tags, use "git push origin –tags".
Pushing changes normally will not push tags to GitHub. You will be asked to checkout this
tagged commit during demo. This is done with "git fetch --all --tags" and "git checkout tag
name". You will be required to pull this tag during the demo. If you forget to tag your
commit or tag your commit late, you will be penalized with –5 points for each day late.
Additional Resources
Under this section are some resources we think will help you and your team streamline the
game creation process. If you have any questions about these resources, contact your TAs
through Ed or office hours.
1. TA website: https://github.gatech.edu/pages/gtobdes/obdes/
2. Ed: https://edstem.org/us/courses/81858/discussion/6876706
3. Android Studio Tutorial Videos (located on canvas --> media gallery).
Submission
You will be graded based on the successful and accurate completion of the design
deliverables and implementation requirements outlined above. To receive credit for Sprint
4, you must submit a demo video showcasing the functionality. The video should clearly
walk through all Sprint 4 features, end-to-end project demo (using 2 scenarios above), any
extra credit features, demonstrate end-to-end testing requirements listed above, run
CheckStyle successfully, walkthrough relevant unit tests, and show functionality of any
extra credit features implemented. It must not exceed 20 minutes, and credit may be
withheld if any functionality is unclear or not shown. Please follow the rubric while recording
your demo video to avoid possible deductions.
Submit a link to the video as a comment on your Canvas assignment. Please ensure the link
does not require any downloads. We highly recommend uploading the video to YouTube and
providing a link. In addition, upload your design deliverables as a combined PDF via the
Canvas assignment and include a link to your GitHub repository. Your repo must be set to
private. Don’t forget to add the shared TA GitHub account to your repo for access. Failure to
follow any of these submission guidelines may result in point deductions.
Academic Integrity
Note on Plagiarism and the Use of AI Tools
We understand that technology and tools, including Artificial Intelligence platforms, have
become readily accessible and can be valuable in various scenarios. However, students
must be cautious about their usage in academic settings. Please refer to the course policy
regarding the use of AI tools for your projects. Using AI to generate or copy content for your
project is not considered collaboration. Leveraging AI to produce work that you present as
your own, without proper citation, is likely crossing the line into academic misconduct. It's
essential to maintain integrity in all academic undertakings. If in doubt, always consult the
course guidelines or reach out to the instructor on Ed Discussion for clarification. If you are
violating academic integrity policies, you WILL be caught and reported to OSI, which could
result in serious consequences.