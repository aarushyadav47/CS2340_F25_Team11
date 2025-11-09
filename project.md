
1
SpendWise – Project Outline
Project Description
SpendWise is a personal finance and savings management mobile app designed to help
users take control of their spending and build better money habits. The app enables
users to log expenses, set and track budgets, and visualize their progress through
intuitive dashboards. With a focus on simplicity and clarity, SpendWise provides users
with the tools to understand where their money goes, stay on top of their financial goals,
and be motivated to save.
Key features include expense logging with categories, personalized budgets, real-time
progress dashboards powered by charts, and group-oriented “Savings Circles” where
friends can collaborate on shared saving challenges. To further support financial
wellness, SpendWise also integrates a budget-specific AI chatbot that provides money-
saving tips, answers FAQs, and gives motivational nudges. App notifications sent to the
device keep users accountable when they miss logs or when budgets are nearing their
limits, helping them stay engaged without the complexity of system notifications.
Your project will be implemented across four sprints. The proceeding descriptions are
basic ideas of what each sprint should cover. The sprint descriptions are subject to
change and requirements may be added or removed. There may also be extra credit
opportunities in certain sprints for certain extra features implemented.
Each sprint builds on the previous and includes UI development, logic integration,
Firebase functionality, and the use of design patterns. Students must follow the sprint
breakdowns and are encouraged to add polish and extra features for extra credit. Below
is a summary of the implementation requirements of the project across the 4 sprints.
We encourage you to review the full project outline and sprint documents in advance to
help avoid confusion and clarify most questions.
Sprint 1: Establishing Foundations and User Interface
Focus: Firebase connection, authentication system, project structure using MVVM
1. Create a welcome/start splash screen with options to Start and Quit the app. This
is the first screen the user sees and should provide a clean entry point into the
application.
2. Connect Android Studio to Firebase. Instructions will be provided in the Sprint 1
assignment. Firebase will be used throughout the semester, starting with
Authentication. Refer to: Firebase Android Setup Guide (Additional help was
provided in Android Studio Workshop #3.)
3. Create a user authentication system using Firebase Authentication:
a. Develop a login screen that allows existing users to log in using their email (not
username) and password.
b. Include an option for new users to navigate to an account creation screen
where they can register using their email and password.
c. Securely store credentials using Firebase’s built-in authentication system.
Use the Firestore user database to store minimal account metadata if needed.
4. Implement MVVM architecture:
a. Set up separate ViewModel classes for each main screen to support
separation of concerns.
b. For this sprint, use dummy/static data in ViewModels (real Firebase data will
be integrated later).
c. Maintain good file structure and logic separation from the beginning.
5. Design and implement a basic navigation structure:
a. Use a bottom navigation bar with five tabs to represent the core modules of
SpendWise: Dashboard, Expense Log, Budgets, Savings Circles, and Chatbot.
b. Ensure smooth navigation between all tabs.
6. Create placeholder screens for the main app modules using consistent UI and naming:
At this point, students are only expected to design the layout and navigation for these
screens using placeholder UI elements and dummy ViewModel data. These screens will
be fully functional in future sprints.
a. Dashboard Screen: Create a placeholder screen. Functionality such as
displaying analytics will be added later.
b. Expense Log Screen: Create a placeholder screen with a “+” button for
adding expenses. Clicking the button should lead to a new Expense Log
Creation screen, which for this sprint can display placeholder text.
Functionality will be added later.
c. Budgets Screen: Create a placeholder screen with a “+” button for creating
budgets. Clicking the button should lead to a placeholder Budget Creation
form, which for this sprint can display placeholder text. Functionality such as
budget creation and tracking will be added later.
d. Savings Circles Screen: Create a placeholder screen. Functionality such as
group saving goals will be added later.
e. Chatbot Screen: Create a placeholder screen. Functionality such as
providing budgeting tips and motivational nudges will be added later.
7. Add consistent visual elements and headers to each screen:
