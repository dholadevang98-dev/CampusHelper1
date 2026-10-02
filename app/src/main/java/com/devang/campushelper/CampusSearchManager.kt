package com.devang.campushelper

object CampusSearchManager {

    enum class Category(val displayName: String, val iconRes: Int, val accentColorRes: Int) {
        ALL("All", R.drawable.ic_search, R.color.splash_accent_primary),
        SYLLABUS("Syllabus", R.drawable.ic_library, R.color.splash_accent_primary),
        NOTICES("Notices", R.drawable.ic_notices, R.color.accent_cyan),
        LIBRARY("Library", R.drawable.ic_library, R.color.accent_emerald),
        CANTEEN("Canteen", R.drawable.ic_canteen, R.color.accent_amber),
        TIMETABLE("Timetable", R.drawable.ic_timetable, R.color.splash_accent_glow),
        FACULTY("Faculty & Labs", R.drawable.ic_person, R.color.accent_sky),
        SERVICES("Services", R.drawable.ic_helpdesk, R.color.accent_rose)
    }

    data class CourseOutcome(
        val id: String,
        val description: String,
        val rbtLevel: String
    )

    data class SyllabusUnit(
        val unitNo: Int,
        val title: String,
        val hours: Int,
        val weightagePercent: Int,
        val topics: List<String>
    )

    data class SyllabusBook(
        val srNo: Int,
        val title: String,
        val author: String,
        val publication: String
    )

    data class SyllabusSubject(
        val code: String,
        val name: String,
        val category: String, // e.g. PCC, PEC-III, MOPEC
        val semester: Int = 5,
        val branch: String = "Information Technology",
        val academicYear: String = "2026-27",
        val credits: Int,
        val lectureHours: Int,
        val tutorialHours: Int,
        val practicalHours: Int,
        val totalMarks: Int,
        val theoryEseMarks: Int,
        val theoryPaMarks: Int,
        val practicalPaMarks: Int,
        val practicalEseMarks: Int,
        val prerequisite: String,
        val rationale: String,
        val courseOutcomes: List<CourseOutcome>,
        val units: List<SyllabusUnit>,
        val suggestedPracticals: List<String>,
        val books: List<SyllabusBook>,
        val sampleProjects: List<String> = emptyList()
    )

    data class SearchItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val category: Category,
        val tag: String,
        val iconRes: Int,
        val accentColorRes: Int,
        val actionText: String,
        val detailsHeader: String,
        val detailsBody: String
    )

    private val CAMPUS_DATABASE = listOf(
        // Notices & Circulars
        SearchItem(
            id = "not_01",
            title = "Mid-Semester Examination Schedule 2026",
            subtitle = "Gujarat Technological University • All Branches",
            category = Category.NOTICES,
            tag = "EXAM",
            iconRes = R.drawable.ic_notices,
            accentColorRes = R.color.accent_cyan,
            actionText = "Download Exam Schedule (PDF)",
            detailsHeader = "Mid-Sem Schedule Details",
            detailsBody = "• Dates: 15th to 22nd October 2026\n• Timing: 10:30 AM to 1:00 PM\n• Hall Tickets: Available on Student Portal from 10th Oct"
        ),
        SearchItem(
            id = "not_02",
            title = "IGNITE 2026 Annual TechFest Registration",
            subtitle = "Campus Hackathon, Web-A-Thon & Circuit Design",
            category = Category.NOTICES,
            tag = "FEST",
            iconRes = R.drawable.ic_notices,
            accentColorRes = R.color.accent_violet,
            actionText = "Register Team Online",
            detailsHeader = "Event Highlights",
            detailsBody = "• Cash prizes worth ₹75,000\n• 24-hr Hackathon in IT Lab 302\n• Free registration for GP Rajkot students"
        ),
        SearchItem(
            id = "not_03",
            title = "GTU Academic Calendar 2026-2027",
            subtitle = "Term dates, submission deadlines, holidays",
            category = Category.NOTICES,
            tag = "ACADEMIC",
            iconRes = R.drawable.ic_notices,
            accentColorRes = R.color.accent_cyan,
            actionText = "View Full Calendar",
            detailsHeader = "Term Highlights",
            detailsBody = "• Term Commencement: August 2026\n• Term End: December 2026\n• Final Practical Submissions: Dec 1-5"
        ),
        SearchItem(
            id = "not_04",
            title = "IT Server & Wi-Fi Maintenance Notice",
            subtitle = "Campus High-Speed Wi-Fi upgrade scheduled",
            category = Category.NOTICES,
            tag = "MAINTENANCE",
            iconRes = R.drawable.ic_notices,
            accentColorRes = R.color.accent_amber,
            actionText = "Check Wi-Fi Status",
            detailsHeader = "Maintenance Schedule",
            detailsBody = "• Date: This Saturday 2:00 PM - 6:00 PM\n• Impact: Lab Wi-Fi & Proxy Servers will restart"
        ),
        SearchItem(
            id = "not_05",
            title = "Digital Gujarat Scholarship 2026",
            subtitle = "SC/ST/SEBC & Post-Matric Scholarship Forms Open",
            category = Category.NOTICES,
            tag = "SCHOLARSHIP",
            iconRes = R.drawable.ic_notices,
            accentColorRes = R.color.accent_emerald,
            actionText = "Apply on Digital Gujarat Portal",
            detailsHeader = "Scholarship Guidelines",
            detailsBody = "• Last Date: 30th November 2026\n• Submit income certificate & fee receipt at Admin Window #4"
        ),

        // Library Books & Resources
        SearchItem(
            id = "lib_01",
            title = "Operating System Concepts (10th Edition)",
            subtitle = "Authors: Silberschatz, Galvin, Gagne • Shelf 14-B",
            category = Category.LIBRARY,
            tag = "AVAILABLE (4)",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_emerald,
            actionText = "Reserve Book Now",
            detailsHeader = "Book Availability",
            detailsBody = "• Total Copies: 6\n• Available: 4 copies on Shelf 14-B\n• Standard issue period: 14 days"
        ),
        SearchItem(
            id = "lib_02",
            title = "Database System Concepts (7th Edition)",
            subtitle = "Authors: Abraham Silberschatz, Henry F. Korth • Shelf 08-A",
            category = Category.LIBRARY,
            tag = "AVAILABLE (2)",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_emerald,
            actionText = "Reserve Book Now",
            detailsHeader = "Book Details",
            detailsBody = "• Standard Reference for GTU DBMS Course\n• Includes SQL, Normalization & Transaction Processing exercises"
        ),
        SearchItem(
            id = "lib_03",
            title = "Computer Networks (5th Edition)",
            subtitle = "Authors: Andrew S. Tanenbaum, David J. Wetherall • Shelf 12-C",
            category = Category.LIBRARY,
            tag = "ISSUED",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_rose,
            actionText = "Put on Waitlist",
            detailsHeader = "Book Status",
            detailsBody = "• All 5 copies currently issued\n• Next copy expected return: Friday 3:00 PM"
        ),
        SearchItem(
            id = "lib_04",
            title = "Central Reading Room & Study Seats",
            subtitle = "Air Conditioned Quiet Study Zone • 2nd Floor",
            category = Category.LIBRARY,
            tag = "38 SEATS FREE",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_emerald,
            actionText = "View Reading Room Status",
            detailsHeader = "Facility Details",
            detailsBody = "• Capacity: 50 Students\n• Current Occupancy: 12 Students (38 Free Seats)\n• Open Timings: 8:00 AM - 8:00 PM"
        ),
        SearchItem(
            id = "lib_05",
            title = "Data Structures Using C and C++",
            subtitle = "Authors: Tenenbaum, Langsam, Augenstein • Shelf 09-D",
            category = Category.LIBRARY,
            tag = "AVAILABLE (6)",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_emerald,
            actionText = "Issue Book (14 Days)",
            detailsHeader = "Core Textbook for Semester 3/4",
            detailsBody = "• Complete algorithms for Stacks, Queues, Linked Lists, Trees & Graphs\n• Available in Main Library Section"
        ),

        // Canteen Menu & Counter
        SearchItem(
            id = "can_01",
            title = "Paneer Butter Masala Gujarati Thali",
            subtitle = "4 Roti, Paneer Subji, Dal, Rice, Salad & Sweet • ₹80",
            category = Category.CANTEEN,
            tag = "POPULAR",
            iconRes = R.drawable.ic_canteen,
            accentColorRes = R.color.accent_amber,
            actionText = "Pre-Order Thali Token (₹80)",
            detailsHeader = "Thali Nutrition & Wait Time",
            detailsBody = "• Freshly prepared daily lunch special\n• Available: 11:30 AM - 2:30 PM\n• Average prep wait: 4 minutes"
        ),
        SearchItem(
            id = "can_02",
            title = "Samosa & Masala Chai Combo",
            subtitle = "2 Hot Crispy Samosas with Green Chutney + Kadak Chai • ₹30",
            category = Category.CANTEEN,
            tag = "HOT & FRESH",
            iconRes = R.drawable.ic_canteen,
            accentColorRes = R.color.accent_amber,
            actionText = "Pre-Order Combo Token (₹30)",
            detailsHeader = "Snack Counter Status",
            detailsBody = "• Ready for instant pickup at Snack Counter #1\n• Fresh batch prepared every 20 minutes"
        ),
        SearchItem(
            id = "can_03",
            title = "Cold Coffee & Veg Grilled Sandwich",
            subtitle = "Thick cold coffee with triple-layer cheese veg sandwich • ₹50",
            category = Category.CANTEEN,
            tag = "BESTSELLER",
            iconRes = R.drawable.ic_canteen,
            accentColorRes = R.color.accent_amber,
            actionText = "Pre-Order Snack Token (₹50)",
            detailsHeader = "Special Item",
            detailsBody = "• Fresh whole wheat or white bread option\n• Quick pickup: Counter #2"
        ),
        SearchItem(
            id = "can_04",
            title = "Main Food Court Counter Status",
            subtitle = "Live token queue & counter rush tracker",
            category = Category.CANTEEN,
            tag = "LOW RUSH (4 MIN)",
            iconRes = R.drawable.ic_canteen,
            accentColorRes = R.color.accent_emerald,
            actionText = "View Live Counter Rush",
            detailsHeader = "Food Court Rush Index",
            detailsBody = "• Current Tokens in Queue: 6\n• Estimated Wait: 4 minutes\n• All 3 billing counters active"
        ),
        SearchItem(
            id = "can_05",
            title = "Chole Bhature Special Platter",
            subtitle = "2 Fluffy Bhature with spicy Amritsari Chole & Pickles • ₹60",
            category = Category.CANTEEN,
            tag = "LUNCH SPECIAL",
            iconRes = R.drawable.ic_canteen,
            accentColorRes = R.color.accent_amber,
            actionText = "Pre-Order Chole Bhature (₹60)",
            detailsHeader = "Lunch Special Platter",
            detailsBody = "• Served hot between 12:00 PM and 3:00 PM\n• Pickup at Counter #3"
        ),

        // Timetable & Labs
        SearchItem(
            id = "time_01",
            title = "Data Structures & Algorithms Lab",
            subtitle = "Prof. Mehta • Lab 302 • Mon & Wed 11:00 AM",
            category = Category.TIMETABLE,
            tag = "SCHEDULED",
            iconRes = R.drawable.ic_timetable,
            accentColorRes = R.color.splash_accent_glow,
            actionText = "Open Lab Syllabus & Code",
            detailsHeader = "Session Details",
            detailsBody = "• Topics: Binary Search Trees & Graph Traversal\n• Lab Assignment 4 submission due today\n• Linux GCC compiler environment"
        ),
        SearchItem(
            id = "time_02",
            title = "Web Technology & Mobile App Design",
            subtitle = "Room 204 • Tues & Thurs 1:30 PM",
            category = Category.TIMETABLE,
            tag = "LECTURE",
            iconRes = R.drawable.ic_timetable,
            accentColorRes = R.color.splash_accent_glow,
            actionText = "View Lecture Notes",
            detailsHeader = "Lecture Details",
            detailsBody = "• Topics: Android Jetpack, Kotlin Coroutines, Navigation Component\n• Project review scheduled for next week"
        ),
        SearchItem(
            id = "time_03",
            title = "Database Management Systems (DBMS)",
            subtitle = "Room 106 • Mon, Wed, Fri 9:30 AM",
            category = Category.TIMETABLE,
            tag = "LECTURE",
            iconRes = R.drawable.ic_timetable,
            accentColorRes = R.color.splash_accent_glow,
            actionText = "View DBMS Assignments",
            detailsHeader = "Course Progress",
            detailsBody = "• Current Chapter: Normalization (1NF, 2NF, 3NF, BCNF)\n• SQL Lab session scheduled for Friday"
        ),
        SearchItem(
            id = "time_04",
            title = "Computer Networks & Cyber Security Lab",
            subtitle = "Lab 304 • Thursday 2:30 PM - 4:30 PM",
            category = Category.TIMETABLE,
            tag = "LAB SESSION",
            iconRes = R.drawable.ic_timetable,
            accentColorRes = R.color.splash_accent_glow,
            actionText = "View Lab Manual",
            detailsHeader = "Networking Lab Practical",
            detailsBody = "• Wireshark packet analysis, Subnetting calculations, Cisco Packet Tracer simulation\n• Lab Incharge: Prof. Trivedi"
        ),

        // Faculty & Contacts
        SearchItem(
            id = "fac_01",
            title = "Prof. H. Mehta (Head of IT Department)",
            subtitle = "IT Dept HOD Office • Extension #301",
            category = Category.FACULTY,
            tag = "AVAILABLE",
            iconRes = R.drawable.ic_person,
            accentColorRes = R.color.accent_sky,
            actionText = "Send Academic Query",
            detailsHeader = "Faculty Office Hours",
            detailsBody = "• Office: HOD Cabin, 3rd Floor IT Block\n• Student Consultation Hours: 3:00 PM - 5:00 PM\n• Email: hod.it@gprajkot.ac.in"
        ),
        SearchItem(
            id = "fac_02",
            title = "Central IT Computer Lab 302",
            subtitle = "60 High-performance Core i7 Systems • GPU Rig",
            category = Category.FACULTY,
            tag = "OPEN FOR STUDENTS",
            iconRes = R.drawable.ic_helpdesk,
            accentColorRes = R.color.accent_sky,
            actionText = "Book Lab Workstation",
            detailsHeader = "Lab Specifications",
            detailsBody = "• Ubuntu 24.04 LTS & Windows 11 Dual Boot\n• Android Studio, VS Code, Python 3.12, Docker installed\n• Lab Assistant: Mr. Trivedi"
        ),
        SearchItem(
            id = "fac_03",
            title = "Prof. K. Dave (Mechanical Engineering HOD)",
            subtitle = "Workshop Building • Room 102",
            category = Category.FACULTY,
            tag = "MECH DEPT",
            iconRes = R.drawable.ic_person,
            accentColorRes = R.color.accent_sky,
            actionText = "Contact Department Desk",
            detailsHeader = "Department Highlights",
            detailsBody = "• CAD/CAM Lab & CNC Machine Workshop\n• Industrial visits coordinator for Semester 5 students"
        ),

        // Services & Grievance
        SearchItem(
            id = "serv_01",
            title = "Lost & Found: Boat Airdopes Case",
            subtitle = "Found in Library 2nd Floor Reading Table 4",
            category = Category.SERVICES,
            tag = "FOUND ITEM",
            iconRes = R.drawable.ic_lost_found,
            accentColorRes = R.color.accent_rose,
            actionText = "Claim This Item",
            detailsHeader = "Item Details",
            detailsBody = "• Deposited at Central Security Cabin\n• Verified by Security Officer on Duty\n• Bring Student ID to claim"
        ),
        SearchItem(
            id = "serv_02",
            title = "Campus Grievance & Helpdesk Desk",
            subtitle = "Submit issues regarding Wi-Fi, Classrooms, or Hostel",
            category = Category.SERVICES,
            tag = "24/7 HELPDESK",
            iconRes = R.drawable.ic_helpdesk,
            accentColorRes = R.color.accent_sky,
            actionText = "File Grievance Ticket",
            detailsHeader = "Support Commitment",
            detailsBody = "• Direct dispatch to Administrative Officer\n• Guaranteed turnaround within 24 hours\n• Ticket tracking with SMS alerts"
        ),
        SearchItem(
            id = "serv_03",
            title = "Security Cabin & Main Gate Helpdesk",
            subtitle = "Visitor passes, parking permits & lost item collection",
            category = Category.SERVICES,
            tag = "MAIN GATE",
            iconRes = R.drawable.ic_lock,
            accentColorRes = R.color.accent_emerald,
            actionText = "Contact Security Desk",
            detailsHeader = "Campus Security",
            detailsBody = "• Emergency Contact: Ext 100\n• Vehicle Parking Sticker issuance on Mon & Wed"
        ),

        // GTU Semester 5 IT Syllabus Subjects
        SearchItem(
            id = "syl_ai_prompt",
            title = "Artificial Intelligence with Prompt Engineering (DI05016011)",
            subtitle = "Semester 5 • IT • 4 Credits • PCC • 150 Total Marks",
            category = Category.SYLLABUS,
            tag = "DI05016011",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.splash_accent_primary,
            actionText = "View Detailed Unit Breakdown",
            detailsHeader = "GTU Syllabus: AI with Prompt Engineering",
            detailsBody = "• Code: DI05016011 | Credits: 4 (L:3, T:0, PR:2) | Category: PCC\n" +
                    "• Exam Scheme: Theory ESE: 70, PA: 30 | Practical PA: 20, ESE: 30 | Total: 150 Marks\n" +
                    "• Unit 1: Foundations of AI & GenAI (6 hrs, 15%) - AI vs ML vs DL, Generative AI tools (ChatGPT, Gemini, DALL-E), NLP basics\n" +
                    "• Unit 2: Basics of Large Language Models (6 hrs, 15%) - Tokens, Embeddings, Next-word prediction, GPT/Claude/LLaMA/Gemini, Hallucination & limitations\n" +
                    "• Unit 3: Prompt Engineering Fundamentals (9 hrs, 20%) - Instruction/Context/Input/Output structure, Zero-shot, Few-shot, Role-based prompting\n" +
                    "• Unit 4: Prompt Engineering Techniques (9 hrs, 20%) - Chain-of-Thought, Prompt chaining, ReAct, Task prompting, Retrieval Augmented Generation (RAG)\n" +
                    "• Unit 5: AI App Dev: GenAI & Agentic AI (15 hrs, 30%) - OpenAI & Gemini APIs, Chatbots, Blog writers, Agentic AI (AutoGPT, CrewAI), Responsible AI & Ethics\n" +
                    "• Recommended Textbooks: Russell & Norvig (AI Modern Approach), Jay Alammar (Hands-On LLMs), DAIR.AI Prompt Guide"
        ),
        SearchItem(
            id = "syl_ai_product",
            title = "AI Product Design (DI05016021)",
            subtitle = "Semester 5 • IT • 4 Credits • PCC • 150 Total Marks",
            category = Category.SYLLABUS,
            tag = "DI05016021",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_cyan,
            actionText = "View Detailed Unit Breakdown",
            detailsHeader = "GTU Syllabus: AI Product Design",
            detailsBody = "• Code: DI05016021 | Credits: 4 (L:3, T:0, PR:2) | Category: PCC\n" +
                    "• Exam Scheme: Theory ESE: 70, PA: 30 | Practical PA: 20, ESE: 30 | Total: 150 Marks\n" +
                    "• Unit 1: Fundamentals of AI Products & Systems (8 hrs, 18%) - AI tool vs product, Data/Model/Interface/Feedback, GenAI vs Analytical AI, Multi-Agent AI\n" +
                    "• Unit 2: Design Thinking & Human-Centred AI (7 hrs, 16%) - Empathy Mapping, User Persona, Customer Journey, UX for AI, Explainable AI (XAI)\n" +
                    "• Unit 3: AI Product Strategy & OpenAI API (8 hrs, 18%) - Product Lifecycle, MVP, Feature Prioritization, SaaS/API Monetization, Business Model Canvas, Token costs\n" +
                    "• Unit 4: AI in Social Media & Digital Experience (10 hrs, 22%) - Content creation, Recommenders, Chatbots, Deepfake risks, No-code AI tools (Figma, Notion AI, Gamma, Tome, Framer)\n" +
                    "• Unit 5: Privacy, Data Governance & Responsible AI (6 hrs, 13%) - Data minimization, Consent, Responsible AI, Indian DPDP Law overview\n" +
                    "• Unit 6: AI Threats & Mitigation Strategies (6 hrs, 13%) - Prompt injection, Model misuse, Phishing, Moderation, Access control, Human-in-the-loop\n" +
                    "• Recommended Textbooks: Chip Huyen (Designing ML Systems, AI Engineering), Martin Kleppmann (DDIA), Tony Fadell (Build)"
        ),
        SearchItem(
            id = "syl_cloud_datacenter",
            title = "Cloud and Data Center Technology (DI05016031)",
            subtitle = "Semester 5 • IT • 4 Credits • PEC-III • 150 Total Marks",
            category = Category.SYLLABUS,
            tag = "DI05016031",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_emerald,
            actionText = "View Detailed Unit Breakdown",
            detailsHeader = "GTU Syllabus: Cloud and Data Center Technology",
            detailsBody = "• Code: DI05016031 | Credits: 4 (L:3, T:0, PR:2) | Category: PEC-III\n" +
                    "• Exam Scheme: Theory ESE: 70, PA: 30 | Practical PA: 20, ESE: 30 | Total: 150 Marks\n" +
                    "• Unit 1: Introduction to Cloud Computing (4 hrs, 8%) - Distributed/Grid/Cluster/Utility/Cloud, IaaS/PaaS/SaaS, Private/Public/Hybrid\n" +
                    "• Unit 2: Virtualization and Hypervisors (9 hrs, 20%) - Hardware/Software/Full/Para/OS Virtualization, Type 1 & 2 Hypervisors, Managing VMs\n" +
                    "• Unit 3: Data Center Architecture (9 hrs, 20%) - Topologies, SDN (Software-Defined Networking), Automation, Infrastructure as Code (IaC)\n" +
                    "• Unit 4: Cloud Storage & Database Services (9 hrs, 20%) - Object/Block/File storage, SQL & NoSQL Cloud DBs, Replication & Durability\n" +
                    "• Unit 5: Cloud Security and Compliance (6 hrs, 14%) - Cloud IAM, Data security, SLAs, DevSecOps\n" +
                    "• Unit 6: Emerging Technologies (8 hrs, 18%) - Mobile cloud, Serverless, Edge/Fog Computing, AI/ML in Cloud, 5G, Kubernetes & Containers\n" +
                    "• Recommended Textbooks: Rajkumar Buyya (Cloud Computing Principles, Mastering Cloud Computing), Barrie Sosinsky (Cloud Bible)"
        ),
        SearchItem(
            id = "syl_c_programming",
            title = "Structured Programming with C (DI05016061)",
            subtitle = "Semester 5 • IT • 4 Credits • PEC-04 • 150 Total Marks",
            category = Category.SYLLABUS,
            tag = "DI05016061",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_amber,
            actionText = "View Detailed Unit Breakdown",
            detailsHeader = "GTU Syllabus: Structured Programming with C",
            detailsBody = "• Code: DI05016061 | Credits: 4 (L:3, T:0, PR:2) | Category: PEC-04\n" +
                    "• Exam Scheme: Theory ESE: 70, PA: 30 | Practical PA: 20, ESE: 30 | Total: 150 Marks\n" +
                    "• Unit 1: Introduction to C Language (5 hrs, 12%) - Flowcharts, Compilation & Execution, Control statements, Storage classes, Type qualifiers\n" +
                    "• Unit 2: Arrays and Pointers (9 hrs, 20%) - 1D/2D Arrays, VLAs, Pointer arithmetic, Double & Function pointers, Multilevel pointers\n" +
                    "• Unit 3: Functions (12 hrs, 28%) - User-defined functions, Categories, Recursion, Variadic functions, Inline functions\n" +
                    "• Unit 4: User-defined Datatypes (12 hrs, 25%) - Structures, Unions, Structure pointers, Preprocessor macros & conditionals, Typedef\n" +
                    "• Unit 5: File Handling in C (7 hrs, 15%) - Create/Open/Read/Write/Close files, Binary files, File error handling\n" +
                    "• Practical Projects: Mini Banking System, Hospital Patient Management, Shop Inventory System, Online Exam Result System\n" +
                    "• Recommended Textbooks: E. Balagurusamy (Programming in ANSI C), Yashavant Kanetkar (Let Us C)"
        ),
        SearchItem(
            id = "syl_emotional_intel",
            title = "Emotional Intelligence & Digital Wellbeing (DI05016081)",
            subtitle = "Semester 5 • IT • 3 Credits • MOPEC • 100 Total Marks",
            category = Category.SYLLABUS,
            tag = "DI05016081",
            iconRes = R.drawable.ic_library,
            accentColorRes = R.color.accent_violet,
            actionText = "View Detailed Unit Breakdown",
            detailsHeader = "GTU Syllabus: Emotional Intelligence & Digital Wellbeing",
            detailsBody = "• Code: DI05016081 | Credits: 3 (L:0, T:2, PR:2) | Category: MOPEC\n" +
                    "• Exam Scheme: Tutorial PA: 50, Practical ESE: 50 | Total: 100 Marks (Practical & Experiential Course)\n" +
                    "• Unit 1: Foundations of EQ & IQ in Professional Life (7 hrs, 22%) - IQ vs EQ, Self-awareness, Emotional regulation, Empathy, Decision-making\n" +
                    "• Unit 2: Emotional Intelligence in Managerial & IT Skills (8 hrs, 28%) - Active listening, Conflict management, Remote teamwork, Digital etiquette\n" +
                    "• Unit 3: Digital Wellbeing & Responsible Tech Practices (7 hrs, 22%) - Screen time management, Social media addiction, Responsible AI, Cyber psychology, Burnout\n" +
                    "• Unit 4: Mental Health, Energy & Sustainable Digital Practices (8 hrs, 28%) - Mental health in IT, Mindfulness, Green computing, Sustainable IT habits\n" +
                    "• Recommended Textbooks: Pushan Kumar Dutta (EI in Digital Era 2025), Daniel Goleman (Emotional Intelligence), SWAYAM/NPTEL courses"
        )
    )

    // ==================== DEDICATED GTU SYLLABUS DATABASE ====================
    private val SYLLABUS_DATABASE = listOf(
        SyllabusSubject(
            code = "DI05016011",
            name = "Artificial Intelligence with Prompt Engineering",
            category = "PCC",
            semester = 5,
            branch = "Information Technology",
            academicYear = "2026-27",
            credits = 4,
            lectureHours = 3,
            tutorialHours = 0,
            practicalHours = 2,
            totalMarks = 150,
            theoryEseMarks = 70,
            theoryPaMarks = 30,
            practicalPaMarks = 20,
            practicalEseMarks = 30,
            prerequisite = "Python programming, Data handling & programming logic, Fundamentals of Machine Learning, and basic understanding of computer applications.",
            rationale = "Modern software systems increasingly rely on AI-powered assistants for coding, content generation, and automation. Prompt Engineering enables developers to effectively interact with and guide LLMs to build simple AI-powered applications.",
            courseOutcomes = listOf(
                CourseOutcome("CO1", "Explain the fundamentals of Artificial Intelligence and Generative AI.", "Understand"),
                CourseOutcome("CO2", "Describe the working principles and limitations of Large Language Models.", "Understand"),
                CourseOutcome("CO3", "Design effective prompts using prompt engineering techniques.", "Apply"),
                CourseOutcome("CO4", "Apply advanced prompting strategies for performing different AI tasks.", "Apply"),
                CourseOutcome("CO5", "Develop simple AI-based applications using APIs and productivity tools.", "Apply")
            ),
            units = listOf(
                SyllabusUnit(
                    unitNo = 1,
                    title = "Foundations of Artificial Intelligence and Generative AI",
                    hours = 6,
                    weightagePercent = 15,
                    topics = listOf(
                        "1.1 Introduction to Artificial Intelligence (Definition, history, AI vs ML vs DL)",
                        "1.2 Types of AI (Narrow AI vs General AI)",
                        "1.3 Applications of AI (Daily life, education, healthcare, cybersecurity)",
                        "1.4 Introduction to Generative AI (Concept, types: Text, Image, Code generation)",
                        "1.5 Generative AI tools (ChatGPT, Google Gemini, DALL-E)",
                        "1.6 Basics of Natural Language Processing (Concept of NLP, role in chatbots and LLMs)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 2,
                    title = "Basics of Large Language Models (LLMs)",
                    hours = 6,
                    weightagePercent = 15,
                    topics = listOf(
                        "2.1 Introduction to Large Language Models (Concept, training data and parameters)",
                        "2.2 Working of LLMs (Tokens, Embeddings, Next word prediction)",
                        "2.3 Popular LLM Models (GPT models, Claude, LLaMA, Gemini)",
                        "2.4 Capabilities and Limitations (Hallucination problem, Context window limitations, Bias, Cost of AI models)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 3,
                    title = "Prompt Engineering Fundamentals",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "3.1 Introduction to Prompt Engineering (Definition, importance, prompt lifecycle)",
                        "3.2 Prompt Structure (Instruction, Context, Input data, Output format)",
                        "3.3 Prompting Methods (Zero-shot, Few-shot, Role-based, Instruction prompting)",
                        "3.4 Prompt Design Best Practices (Writing effective prompts, testing & evaluation, iterative improvement)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 4,
                    title = "Prompt Engineering Techniques",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "4.1 Prompting Techniques (Chain-of-Thought, Prompt chaining, Self-consistency, ReAct prompting)",
                        "4.2 Step-by-step reasoning prompts",
                        "4.3 Prompt Engineering for Tasks (Text summarization, Content generation, Code generation, QA, Translation)",
                        "4.4 Retrieval Augmented Generation (Concept of RAG, using external knowledge sources)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 5,
                    title = "AI application development: Generative AI, Agentic AI",
                    hours = 15,
                    weightagePercent = 30,
                    topics = listOf(
                        "5.1 Using AI Tools for Productivity (Writing emails, Generating reports, Creating presentations)",
                        "5.2 AI in Software Development (Code generation, Code explanation, Code documentation)",
                        "5.3 AI for Debugging Code (Finding errors, Fixing bugs)",
                        "5.4 Introduction to AI APIs (Concept of APIs, OpenAI API, Gemini API)",
                        "5.5 Developing AI Applications (AI chatbot, AI blog writer, AI document summarizer, AI question generator)",
                        "5.6 Introduction to Agentic AI (Concept of AI agents, Autonomous execution, AutoGPT, CrewAI)",
                        "5.7 Responsible AI (Ethical usage, Bias & fairness, Data privacy, Risks & limitations)"
                    )
                )
            ),
            suggestedPracticals = listOf(
                "Use Generative AI tools to perform different tasks & document applications across domains",
                "Perform basic NLP-based tasks (sentiment analysis, text classification) using AI tools or Python",
                "Perform experiments to analyze LLM behavior by testing prompt variations & consistency",
                "Evaluate capabilities & limitations of LLMs by identifying hallucinations & ambiguity",
                "Design and refine prompts for email writing and concept explanation",
                "Apply prompting techniques: zero-shot, few-shot, and role-based prompting",
                "Apply advanced prompting techniques (Chain-of-Thought, prompt chaining)",
                "Perform task-based prompt engineering for summarization, blog & code generation",
                "Use AI tools for software development tasks (code generation, debugging, documentation)",
                "Develop a simple AI chatbot using API integration (OpenAI/Gemini) with Python",
                "Build a document-based question-answering system using AI APIs (Basic RAG concept)",
                "Develop an AI-based application (Study Assistant, Resume Generator, Blog writer, Coding assistant)"
            ),
            books = listOf(
                SyllabusBook(1, "Artificial Intelligence: A Modern Approach", "Stuart Russell, Peter Norvig", "Pearson Education, May 2022, ISBN-10: 9356063575"),
                SyllabusBook(2, "Hands-On Large Language Models", "Jay Alammar, Maarten Grootendorst", "O'Reilly Media, October 2024, ISBN-10: 1098150961"),
                SyllabusBook(3, "Natural Language Processing with Python", "Steven Bird, Ewan Klein, Edward Loper", "O'Reilly Media, July 2009, ISBN-10: 0596516495"),
                SyllabusBook(4, "Machine Learning with Python Cookbook", "Chris Albon", "O'Reilly Media, March 2018, ISBN-10: 9781491989388"),
                SyllabusBook(5, "Prompt Engineering Guide", "DAIR.AI", "Online Resource (promptingguide.ai)")
            ),
            sampleProjects = listOf(
                "AI Study Assistant for students",
                "AI Resume & Cover Letter Generator",
                "AI Summarization Tool",
                "AI Personal Productivity Assistant"
            )
        ),

        SyllabusSubject(
            code = "DI05016021",
            name = "AI Product Design",
            category = "PCC",
            semester = 5,
            branch = "Information Technology",
            academicYear = "2026-27",
            credits = 4,
            lectureHours = 3,
            tutorialHours = 0,
            practicalHours = 2,
            totalMarks = 150,
            theoryEseMarks = 70,
            theoryPaMarks = 30,
            practicalPaMarks = 20,
            practicalEseMarks = 30,
            prerequisite = "Fundamental understanding of computers and IT, basic internet applications, problem-solving techniques, and introductory awareness of AI/ML.",
            rationale = "Introduces students to the fundamentals of AI product design, design thinking, human-centred AI systems, generative AI integration, monetization strategies, and responsible AI governance.",
            courseOutcomes = listOf(
                CourseOutcome("CO1", "Describe the fundamental concepts of AI products and AI models.", "Understand"),
                CourseOutcome("CO2", "Use design thinking principles to develop user-centric AI solutions.", "Apply"),
                CourseOutcome("CO3", "Utilize concepts of AI product lifecycle, feature prioritization, and business models to outline AI product strategies.", "Apply"),
                CourseOutcome("CO4", "Discuss the applications of AI in social media, content creation, and recommendation systems along with associated risks.", "Understand"),
                CourseOutcome("CO5", "Explain data privacy concepts, governance principles, and responsible AI practices.", "Understand"),
                CourseOutcome("CO6", "Apply knowledge of AI threats to identify appropriate mitigation strategies in basic scenarios.", "Apply")
            ),
            units = listOf(
                SyllabusUnit(
                    unitNo = 1,
                    title = "Fundamentals of AI Products & Emerging AI Systems",
                    hours = 8,
                    weightagePercent = 18,
                    topics = listOf(
                        "1.1 Introduction to AI products",
                        "1.2 Difference between AI tool and AI product",
                        "1.3 Components of AI systems (Data, Model, Interface, Feedback loop)",
                        "1.4 Overview of Generative AI",
                        "1.5 Introduction to Large Language Models (LLMs)",
                        "1.6 Types of AI models: Text, Image, Speech, Embeddings",
                        "1.7 Generative AI vs Analytical AI",
                        "1.8 AI model selection for product use",
                        "1.9 Introduction to Multi-Agent AI (basic idea and applications)",
                        "1.10 Human-in-the-loop systems",
                        "1.11 Basic AI system architecture (conceptual diagram)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 2,
                    title = "Design Thinking & Human-Centred AI",
                    hours = 7,
                    weightagePercent = 16,
                    topics = listOf(
                        "2.1 Introduction to Design Thinking",
                        "2.2 Empathy Mapping",
                        "2.3 User Persona Development",
                        "2.4 Problem Statement Writing",
                        "2.5 Customer Journey Mapping",
                        "2.6 UX principles for AI systems",
                        "2.7 Human-AI interaction design",
                        "2.8 Bias in AI systems (basic understanding)",
                        "2.9 Explainable AI (conceptual overview)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 3,
                    title = "AI Product Strategy & OpenAI Integration",
                    hours = 8,
                    weightagePercent = 18,
                    topics = listOf(
                        "3.1 Product lifecycle management",
                        "3.2 Minimum Viable Product (MVP)",
                        "3.3 Feature prioritization",
                        "3.4 AI monetization models (SaaS, API-based, subscription)",
                        "3.5 Business Model Canvas",
                        "3.6 Introduction to API concept",
                        "3.7 Understanding AI APIs (e.g., OpenAI API – concepts only)",
                        "3.8 Basic API key and security awareness",
                        "3.9 Cost awareness in AI products (token concept)",
                        "3.10 Basic AI evaluation methods (Output quality, user feedback, testing checklist)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 4,
                    title = "AI in Social Media & Digital Experience",
                    hours = 10,
                    weightagePercent = 22,
                    topics = listOf(
                        "4.1 AI in content creation",
                        "4.2 AI recommendation systems (concept)",
                        "4.3 AI chatbots for customer service",
                        "4.4 AI-driven ad targeting (concept)",
                        "4.5 Virtual influencers and AI avatars",
                        "4.6 Risks in Social Media AI (Deep fakes, Fake news, Bot networks, Algorithm manipulation)",
                        "4.7 AI Tools & Prototyping (No-code AI tools, presentation tools, Logo/content gen, Basic UI mockup, Demo video)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 5,
                    title = "Privacy, Data Governance & Responsible AI",
                    hours = 6,
                    weightagePercent = 13,
                    topics = listOf(
                        "5.1 Data privacy concepts",
                        "5.2 Personal vs. sensitive data",
                        "5.3 Data lifecycle in AI systems",
                        "5.4 Data minimization principle",
                        "5.5 Data anonymization (basic concept)",
                        "5.6 Transparency and user consent",
                        "5.7 Responsible AI principles",
                        "5.8 Overview of Indian data protection law (basic idea)",
                        "5.9 Basic AI governance principles (organizational level overview)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 6,
                    title = "AI Threats & Mitigation Strategies",
                    hours = 6,
                    weightagePercent = 13,
                    topics = listOf(
                        "6.1 AI Threats (Data breaches, Prompt injection attacks, Model misuse, Bias & discrimination, AI phishing/scams, Deep-fakes)",
                        "6.2 Mitigation Strategies (Input validation, Output moderation, Secure API key handling, Access control basics, Basic logging & monitoring, Human-in-the-loop review, AI risk assessment checklist)"
                    )
                )
            ),
            suggestedPracticals = listOf(
                "Define AI product idea, problem statement, target users, and minimum 3 core features",
                "Draw conceptual AI system architecture showing Data, Model, Interface, and Feedback Loop",
                "Identify and describe Data components, classify data types, and identify risks",
                "Develop User Persona, Empathy Map, and Customer Journey Map",
                "Design AI interaction flow and basic UI wireframe (Figma/Canva)",
                "Define Minimum Viable Product (MVP) features and prepare feature prioritization matrix",
                "Develop Business Model Canvas and identify AI monetization strategy",
                "Prepare AI integration plan including API usage, cost awareness, and security",
                "Design AI-based social media campaign and generate promotional content",
                "Develop prototype using no-code tools and prepare demo video (2–3 minutes)",
                "Draft Data Protection Plan and AI Ethical Usage Policy",
                "Perform AI risk assessment and design mitigation strategy"
            ),
            books = listOf(
                SyllabusBook(1, "Designing Machine Learning Systems", "Chip Huyen", "O'Reilly, 2022, ISBN: 978-1-098-10796-3"),
                SyllabusBook(2, "AI Engineering", "Chip Huyen", "O'Reilly, 2025, ISBN: 978-1098166304"),
                SyllabusBook(3, "Designing Data-Intensive Applications", "Martin Kleppmann", "O'Reilly, 2017, ISBN: 978-1-449-37332-0"),
                SyllabusBook(4, "Build", "Tony Fadell", "Penguin Random House, 2022, ISBN: 9781787634114"),
                SyllabusBook(5, "The Alignment Problem", "Brian Christian", "W. W. Norton, 2020, ISBN: 978-0393635829"),
                SyllabusBook(6, "Lean Product and Lean Analytics", "Ben Yoskovitz & Alistair Croll", "O'Reilly, 2019, ISBN: 978-1-449-33567-0")
            )
        ),

        SyllabusSubject(
            code = "DI05016031",
            name = "Cloud and Data Center Technology",
            category = "PEC-III",
            semester = 5,
            branch = "Information Technology",
            academicYear = "2026-27",
            credits = 4,
            lectureHours = 3,
            tutorialHours = 0,
            practicalHours = 2,
            totalMarks = 150,
            theoryEseMarks = 70,
            theoryPaMarks = 30,
            practicalPaMarks = 20,
            practicalEseMarks = 30,
            prerequisite = "Basic understanding of computer systems, operating systems, networking fundamentals, databases, and web technologies.",
            rationale = "Equips students with foundational knowledge in cloud computing and data center technologies including virtualization, cloud architectures, storage, security, and modern containers.",
            courseOutcomes = listOf(
                CourseOutcome("01", "Understand the concept of Cloud architecture and its model.", "Understand"),
                CourseOutcome("02", "Apply the virtualization concept with its types.", "Apply"),
                CourseOutcome("03", "Understand Data Center Architecture.", "Understand"),
                CourseOutcome("04", "Learn and use Cloud Storage and Database Services.", "Apply"),
                CourseOutcome("05", "Explain Cloud Security and Compliance.", "Apply"),
                CourseOutcome("06", "Understand and implement emerging technologies with Cloud Computing.", "Apply")
            ),
            units = listOf(
                SyllabusUnit(
                    unitNo = 1,
                    title = "Introduction to cloud computing",
                    hours = 4,
                    weightagePercent = 8,
                    topics = listOf(
                        "1.1 Trends in computing (Distributed, Grid, Cluster, Utility, Cloud)",
                        "1.2 Define Cloud Computing (Characteristics, Roots of cloud computing)",
                        "1.3 Cloud Service Model (Cloud Architecture, IaaS, PaaS, SaaS)",
                        "1.4 Deployment Models (Private, Community, Public, Hybrid cloud)",
                        "1.5 Desired Features of a Cloud",
                        "1.6 Pros and Cons of Cloud computing",
                        "1.7 Applications of cloud computing"
                    )
                ),
                SyllabusUnit(
                    unitNo = 2,
                    title = "Virtualization and Hypervisors",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "2.1 Introduction to Cloud virtualization",
                        "2.2 Characteristics and overview of virtualization",
                        "2.3 Types of Cloud Virtualization (Hardware, Software, Full, Para, Partial, OS level virtualization)",
                        "2.4 Hypervisors and Virtual Machines (Type 1 and Type 2, Creating and managing VMs)",
                        "2.5 Virtualization of Clusters and data centers automation"
                    )
                ),
                SyllabusUnit(
                    unitNo = 3,
                    title = "Data Center Architecture",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "3.1 Data Center Fundamentals (Historical perspective, Key components)",
                        "3.2 Data Center Networking (Topologies, SDN - Software-Defined Networking in data center)",
                        "3.3 Data Center Automation and Scaling (Automation, Infrastructure as Code / IaC tools, Scalability & elasticity)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 4,
                    title = "Cloud Storage and Database Services",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "4.1 Cloud Storage Solutions (Object, block, file storage, Data consistency & durability)",
                        "4.2 Cloud Databases (Types: SQL, NoSQL, Data scaling and replication)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 5,
                    title = "Cloud Security and Compliance",
                    hours = 6,
                    weightagePercent = 14,
                    topics = listOf(
                        "5.1 Security in the Cloud (Challenges, Identity & Access Management, Access control & authentication)",
                        "5.2 Data Security in Cloud (Technologies for Data Security)",
                        "5.3 Securing Private and Public Cloud Architecture (SLA Metrics, DevSecOps)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 6,
                    title = "Emerging Technologies with Cloud Computing",
                    hours = 8,
                    weightagePercent = 18,
                    topics = listOf(
                        "6.1 Mobile cloud computing (MCC)",
                        "6.2 Sensor and IOT cloud",
                        "6.3 Serverless Computing",
                        "6.4 Edge and Fog Computing",
                        "6.5 AI and Machine Learning with Cloud Computing",
                        "6.6 Distributed Ledger Technology (DLT) with Cloud computing",
                        "6.7 5G and Cloud-Native Networking",
                        "6.8 Kubernetes and Containers"
                    )
                )
            ),
            suggestedPracticals = listOf(
                "Sketch out & analyze architecture of OpenStack/Eucalyptus/OpenNebula/KVM",
                "Create a Cloud Organization in AWS/Google Cloud/OpenStack with Role-based access control",
                "Install VirtualBox/VMware workstation with Linux/Windows guest OS",
                "Create desktop Virtualization using Chrome Remote Desktop",
                "Setup virtual SDN lab using Mininet Environment (mininet.org)",
                "Study and comparison on cloud databases (Amazon RDS, Cloud SQL, Azure SQL, Mongo Atlas)",
                "Study and comparison on cloud storage (Amazon S3, Google Cloud Storage, Azure Blob Storage)",
                "Simulate secure file sharing using CloudSim open-source framework",
                "Implement secure object storage with access control and encryption",
                "Creating and Executing Your First Container Using Docker platform"
            ),
            books = listOf(
                SyllabusBook(1, "Cloud computing, Principles and Paradigm", "Rajkumar Buyya, J. Broberg, A. Goscinski", "John Wiley & Sons, ISBN: 978-0-470-88799-8"),
                SyllabusBook(2, "Mastering Cloud Computing", "Rajkumar Buyya, Christian Vecchiola, S Thamarai Selvi", "McGraw Hill, ISBN: 978-1-25-902995-0"),
                SyllabusBook(3, "Cloud Computing Bible", "Barrie Sosinsky", "Wiley Publishing, ISBN: 978-0-470-90356-8"),
                SyllabusBook(4, "Cloud Computing: A Practical Approach", "Anthony T. Velte, Toby J. Velte, Robert Elsenpeter", "McGraw Hill, ISBN: 978-0-07-068351-8"),
                SyllabusBook(5, "Cloud Data Centers and Cost Modeling", "Caesar Wu, Rajkumar Buyya", "Elsevier Science, ISBN: 978-0-12-801413-4")
            ),
            sampleProjects = listOf(
                "Create a cloud-based web/mobile application",
                "Case study report on Amazon Cloud Services / Google Cloud",
                "Host a static website using AWS S3 / CloudFront or equivalent"
            )
        ),

        SyllabusSubject(
            code = "DI05016061",
            name = "Structured Programming with C",
            category = "PEC-04",
            semester = 5,
            branch = "Information Technology",
            academicYear = "2026-27",
            credits = 4,
            lectureHours = 3,
            tutorialHours = 0,
            practicalHours = 2,
            totalMarks = 150,
            theoryEseMarks = 70,
            theoryPaMarks = 30,
            practicalPaMarks = 20,
            practicalEseMarks = 30,
            prerequisite = "Basic computer skills, logical thinking, and problem-solving abilities.",
            rationale = "Introductory Computer Programming Course focusing on developing logical thinking and programming skills using C language for scientific, research, and business purposes.",
            courseOutcomes = listOf(
                CourseOutcome("01", "Write C programs to input and output data in prescribed formats with/without control structures.", "Understand"),
                CourseOutcome("02", "Design C programs using arrays and pointers.", "Apply"),
                CourseOutcome("03", "Implement user-defined functions.", "Apply"),
                CourseOutcome("04", "Create user-defined datatypes like a structure and a union.", "Apply"),
                CourseOutcome("05", "Execute file and I/O operations in C.", "Apply")
            ),
            units = listOf(
                SyllabusUnit(
                    unitNo = 1,
                    title = "Introduction to C Language",
                    hours = 5,
                    weightagePercent = 12,
                    topics = listOf(
                        "1.1 Art of Programming with Flowchart (Basic notation, examples, advantages)",
                        "1.2 C language introduction (History, importance, basic structure)",
                        "1.3 Process of compiling and executing a C Program",
                        "1.4 Header files, Library functions",
                        "1.5 Control Statements (if-else, switch-case, entry & exit controlled loops)",
                        "1.6 Storage Classes (Automatic, External, Static, Register)",
                        "1.7 Type Qualifiers (const, volatile, restrict)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 2,
                    title = "Arrays and Pointers",
                    hours = 9,
                    weightagePercent = 20,
                    topics = listOf(
                        "2.1 Arrays: 1D Array, Multi-dimensional array",
                        "2.2 Variable Length Arrays, Flexible Array Members",
                        "2.3 Advanced Control Flow (goto, NULL statement, comma operator, setjmp & longjmp)",
                        "2.4 Pointers (Initialize, dereference, size, NULL, Void, Wild, Dangling pointers)",
                        "2.5 Advanced Pointers (Double pointers, Function pointers, Pointer to Function)",
                        "2.6 Multilevel Pointers"
                    )
                ),
                SyllabusUnit(
                    unitNo = 3,
                    title = "Functions",
                    hours = 12,
                    weightagePercent = 28,
                    topics = listOf(
                        "3.1 Need for functions, Definition, Elements of User-defined Functions",
                        "3.2 Function Declaration, Return Values and Types, Function Calls",
                        "3.3 Category of Functions (No args no return, args no return, args with return, no args with return)",
                        "3.4 Passing Arrays to Functions, Recursion, Scope, Visibility, and Lifetime of Variables",
                        "3.5 Advanced Concepts (Variadic Functions, va_copy, Inline Functions, _Noreturn Functions)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 4,
                    title = "User-defined Datatypes",
                    hours = 12,
                    weightagePercent = 25,
                    topics = listOf(
                        "4.1 User-defined datatypes (Unions, C Structures, Access & Initialize members, Copy Structure, Passing Structure to Functions, typedef, Structure Pointer)",
                        "4.2 The Preprocessor (Conditional Compilation)",
                        "4.3 Include guards, #undef, #pragma, #error",
                        "4.4 Macros: Overview, Macros vs. Functions",
                        "4.5 Preprocessor Operators, Predefined Macros, Creating Macros",
                        "4.6 Advanced Data Types (#define constants, typedef, Complex numbers, Designated Initializers)"
                    )
                ),
                SyllabusUnit(
                    unitNo = 5,
                    title = "File Handling in C",
                    hours = 7,
                    weightagePercent = 15,
                    topics = listOf(
                        "5.1 Create, Name, Open, Read, Write, Close a File",
                        "5.2 File Opening Modes, Accessing File Pointer",
                        "5.3 Read and Write in a Binary File, File error handling"
                    )
                )
            ),
            suggestedPracticals = listOf(
                "Write a simple C program to print Hello World",
                "Write C program using only printf to display pyramid & number patterns",
                "Store marks of N students using array and pointers to calculate average, highest & lowest",
                "Perform addition, subtraction, and multiplication of matrices using 2D arrays & pointer arithmetic",
                "Use pointers and malloc() to calculate total salary expense and highest salary",
                "Create program using pointer to pointer to modify variable values through multiple levels",
                "Create a menu-driven calculator using separate functions for add, subtract, multiply, divide",
                "Implement factorial and Fibonacci using recursion vs iterative approach",
                "Pass array to function to compute sum, average, and count even/odd numbers",
                "Create a function that accepts variable number of arguments (stdarg.h)",
                "Use struct to store Name, Roll No, Marks with Add, Display, Search operations",
                "Create union for storing int, float, char demonstrating memory sharing",
                "Macro vs function for square calculation performance comparison",
                "Store student records in file (Write, Read, Append)",
                "Store employee data struct using a binary file with Add, Search, Modify record"
            ),
            books = listOf(
                SyllabusBook(1, "Programming in ANSI C", "E. Balagurusamy", "McGraw Hills Education, 2019, ISBN: 9789351343202"),
                SyllabusBook(2, "Let us 'C'", "Yashavant Kanetkar", "BPB Publication, 2020, ISBN: 978-9389845686"),
                SyllabusBook(3, "Programming with ANSI and Turbo C", "Ashok N. Kamthane", "Pearson Education, 2008, ISBN: 978-8131704370"),
                SyllabusBook(4, "Introduction to C Programming", "Reema Thareja", "Oxford University Press, 2018, ISBN: 9780199492282"),
                SyllabusBook(5, "C in Depth", "Srivastava Deepali", "BPB Publications, 2022, ISBN: 9788183330480")
            ),
            sampleProjects = listOf(
                "Mini Banking System (Structures, Functions, Arrays, Persistent File Handling)",
                "Hospital Patient Management System (Patient ID, Name, Disease, Save/Retrieve in File)",
                "Inventory Management System for Shop (Product ID, Price, Dynamic Allocation, Update Stock)",
                "Online Examination Result Processing System (Roll No, 5 Subject Marks, Topper calculation)"
            )
        ),

        SyllabusSubject(
            code = "DI05016081",
            name = "Emotional Intelligence & Digital Wellbeing",
            category = "MOPEC",
            semester = 5,
            branch = "Information Technology",
            academicYear = "2026-27",
            credits = 3,
            lectureHours = 0,
            tutorialHours = 2,
            practicalHours = 2,
            totalMarks = 100,
            theoryEseMarks = 0,
            theoryPaMarks = 0,
            practicalPaMarks = 50,
            practicalEseMarks = 50,
            prerequisite = "Basic understanding of human behavior & emotions, communication skills, computer literacy, and willingness for self-reflection.",
            rationale = "Equips students with essential emotional intelligence and digital wellbeing skills required to succeed in modern IT environments, manage stress, maintain work-life balance, and adopt sustainable technology practices.",
            courseOutcomes = listOf(
                CourseOutcome("CO1", "Explain the concepts of IQ and EQ, and apply self-awareness, emotional regulation, and empathy.", "Understand/Apply"),
                CourseOutcome("CO2", "Demonstrate effective communication, teamwork, conflict management, and professional behavior.", "Apply"),
                CourseOutcome("CO3", "Apply ethical and responsible practices for digital wellbeing, and analyse digital behaviour and screen usage.", "Apply/Analyse"),
                CourseOutcome("CO4", "Apply strategies to manage stress, maintain mental health, and adopt sustainable green digital practices.", "Apply")
            ),
            units = listOf(
                SyllabusUnit(
                    unitNo = 1,
                    title = "Foundations of EQ & IQ in Professional Life",
                    hours = 7,
                    weightagePercent = 22,
                    topics = listOf(
                        "1.1 Introduction to Intelligence Quotient (IQ) and Emotional Intelligence (EQ)",
                        "1.2 Difference between IQ and EQ",
                        "1.3 Role and Importance of EQ in Management and IT Professions",
                        "1.4 Self-Awareness and Self-Assessment Techniques",
                        "1.5 Emotional Regulation and Stress Management Strategies",
                        "1.6 Empathy and Interpersonal Relationships in Professional Life",
                        "1.7 Emotionally Balanced Decision-Making",
                        "1.8 Leadership Skills and Emotional Intelligence"
                    )
                ),
                SyllabusUnit(
                    unitNo = 2,
                    title = "Emotional Intelligence in Managerial & IT Skills",
                    hours = 8,
                    weightagePercent = 28,
                    topics = listOf(
                        "2.1 Effective Communication Skills and Active Listening",
                        "2.2 Conflict Management in Digital Work Environments",
                        "2.3 Team Collaboration in Remote Work Settings",
                        "2.4 Constructive Handling of Criticism and Feedback",
                        "2.5 Professional Ethics and Workplace Conduct",
                        "2.6 Digital Communication Etiquette (Email, Chat, and Virtual Meetings)",
                        "2.7 Time Management and Personal Productivity",
                        "2.8 Emotional Resilience in High-Pressure IT Environments"
                    )
                ),
                SyllabusUnit(
                    unitNo = 3,
                    title = "Digital Wellbeing and Responsible Technology Practices",
                    hours = 7,
                    weightagePercent = 22,
                    topics = listOf(
                        "3.1 Concept of Digital Wellbeing",
                        "3.2 Strategies for Effective Screen Time Management",
                        "3.3 Social Media Addiction and Digital Distractions",
                        "3.4 Application of Artificial Intelligence in Professional Life",
                        "3.5 Responsible and Ethical Use of Artificial Intelligence",
                        "3.6 Fundamentals of Cyber Psychology",
                        "3.7 Digital Burnout and Its Prevention",
                        "3.8 Online Safety and Privacy Awareness"
                    )
                ),
                SyllabusUnit(
                    unitNo = 4,
                    title = "Mental Health, Energy & Sustainable Digital Practices",
                    hours = 8,
                    weightagePercent = 28,
                    topics = listOf(
                        "4.1 Awareness of Mental Health in the IT Sector",
                        "4.2 Stress, Anxiety, and Digital Overload Management",
                        "4.3 Strategies for Work-Life Balance",
                        "4.4 Mindfulness and Relaxation Techniques",
                        "4.5 Impact of Screen Exposure on Physical Health",
                        "4.6 Energy Conservation in IT Systems",
                        "4.7 Fundamentals of Green Computing",
                        "4.8 Sustainable Technology Practices"
                    )
                )
            ),
            suggestedPracticals = listOf(
                "Conduct an Emotional Intelligence (EQ) self-assessment test and interpret the results",
                "Perform a role-play on conflict management in academic or workplace scenarios",
                "Analyze a case study on stress management in IT project teams and suggest solutions",
                "Conduct a mock digital meeting using appropriate virtual communication tools",
                "Practice giving and receiving constructive feedback in a structured session",
                "Participate in a group discussion on challenges in remote team collaboration",
                "Record and analyze personal screen time over a specified period",
                "Analyze the use of AI tools in academic or daily activities",
                "Evaluate the benefits and ethical implications of AI tool usage",
                "Design a personal digital detox plan for improving digital wellbeing",
                "Practice meditation and focus exercises for improving concentration",
                "Demonstrate mindfulness techniques for stress reduction and relaxation",
                "Analyze a case study on sustainable practices adopted by a green IT company",
                "Evaluate energy conservation methods used in IT systems",
                "Prepare a checklist of energy-saving digital habits for sustainable technology use"
            ),
            books = listOf(
                SyllabusBook(1, "Emotional Intelligence in the Digital Era", "Pushan Kumar Dutta", "CRC Press / Routledge, 2025 (1st Edition), ISBN: 978-1032703695"),
                SyllabusBook(2, "The Role of Emotional Intelligence and AI in Organizations", "Jay Kumar Pandey, Mritunjay Rai, Samrat Ray", "Year 2025, ISBN: 978-1041000716"),
                SyllabusBook(3, "Emotional Intelligence: Why It Can Matter More Than IQ", "Daniel Goleman", "Bantam Books, New York, 1995, ISBN: 9780553383713"),
                SyllabusBook(4, "Digital Well-Being in Modern World", "Arul Joseph E., Padmasundari S.", "IJIP, Vol 11, Jan-Mar 2023")
            ),
            sampleProjects = listOf(
                "Prepare a report on self-assessment of Emotional Intelligence (EQ) with presentation",
                "Conduct a group activity on communication & teamwork and present findings",
                "Analyze personal digital behavior and screen usage with improvement strategies",
                "Presentation on stress management, mental health, and work-life balance",
                "Develop a personal improvement plan for emotional intelligence & digital habits"
            )
        )
    )

    /**
     * Returns the complete GTU Syllabus Database for Semester 5 IT
     */
    fun getSyllabusDatabase(): List<SyllabusSubject> = SYLLABUS_DATABASE

    /**
     * Looks up a syllabus subject by its GTU course code or keywords
     */
    fun getSyllabusByCode(subjectCode: String): SyllabusSubject? {
        val clean = subjectCode.trim().lowercase()
        return SYLLABUS_DATABASE.find {
            it.code.lowercase() == clean ||
            it.name.lowercase().contains(clean) ||
            it.category.lowercase() == clean
        }
    }

    /**
     * Searches the syllabus database by keyword matching units, topics, and course names
     */
    fun searchSyllabus(query: String): List<SyllabusSubject> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return SYLLABUS_DATABASE

        val tokens = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return SYLLABUS_DATABASE.filter { subject ->
            val corpus = buildString {
                append("${subject.code} ${subject.name} ${subject.category} ${subject.prerequisite} ${subject.rationale} ")
                subject.units.forEach { unit ->
                    append("${unit.title} ")
                    unit.topics.forEach { topic -> append("$topic ") }
                }
                subject.suggestedPracticals.forEach { append("$it ") }
            }.lowercase()

            tokens.all { token -> corpus.contains(token) } || corpus.contains(clean)
        }
    }

    /**
     * Formats a comprehensive textual summary of a GTU course syllabus
     */
    fun getSyllabusSummaryFormatted(subject: SyllabusSubject): String {
        return buildString {
            append("GUJARAT TECHNOLOGICAL UNIVERSITY\n")
            append("Program: Diploma Engineering • Branch: ${subject.branch}\n")
            append("Semester: ${subject.semester} • Category: ${subject.category} • Academic Year: ${subject.academicYear}\n")
            append("Subject Code: ${subject.code} | Credits: ${subject.credits}\n")
            append("Teaching Scheme: L:${subject.lectureHours}, T:${subject.tutorialHours}, PR:${subject.practicalHours}\n")
            append("Evaluation: Theory (${subject.theoryEseMarks}+${subject.theoryPaMarks}), Practical (${subject.practicalPaMarks}+${subject.practicalEseMarks}) = Total ${subject.totalMarks} Marks\n\n")
            append("--- COURSE OUTCOMES ---\n")
            subject.courseOutcomes.forEach { co ->
                append("• [${co.id}] ${co.description} (${co.rbtLevel})\n")
            }
            append("\n--- COURSE UNITS & TOPICS ---\n")
            subject.units.forEach { unit ->
                append("Unit ${unit.unitNo}: ${unit.title} (${unit.hours} Hrs, ${unit.weightagePercent}%)\n")
                unit.topics.forEach { topic ->
                    append("   $topic\n")
                }
            }
            if (subject.suggestedPracticals.isNotEmpty()) {
                append("\n--- SUGGESTED PRACTICAL EXPERIMENTS ---\n")
                subject.suggestedPracticals.forEachIndexed { index, pr ->
                    append("${index + 1}. $pr\n")
                }
            }
            if (subject.books.isNotEmpty()) {
                append("\n--- REFERENCES / TEXTBOOKS ---\n")
                subject.books.forEach { b ->
                    append("${b.srNo}. ${b.title} by ${b.author} (${b.publication})\n")
                }
            }
        }
    }

    /**
     * Searches database across all fields with multi-word matching and category filtering
     */
    fun search(query: String, filterCategory: Category = Category.ALL): List<SearchItem> {
        val cleanQuery = query.trim().lowercase()

        if (cleanQuery.isEmpty()) {
            return if (filterCategory == Category.ALL) {
                CAMPUS_DATABASE
            } else {
                CAMPUS_DATABASE.filter { it.category == filterCategory }
            }
        }

        val tokens = cleanQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }

        return CAMPUS_DATABASE.filter { item ->
            val matchesCategory = (filterCategory == Category.ALL || item.category == filterCategory)
            if (!matchesCategory) return@filter false

            val searchTarget = "${item.title} ${item.subtitle} ${item.tag} ${item.detailsBody} ${item.category.displayName}".lowercase()

            // Match if all search tokens appear, or full query appears
            tokens.all { token -> searchTarget.contains(token) } || searchTarget.contains(cleanQuery)
        }.sortedByDescending { item ->
            val titleLower = item.title.lowercase()
            if (titleLower.startsWith(cleanQuery)) 4
            else if (titleLower.contains(cleanQuery)) 3
            else if (item.subtitle.lowercase().contains(cleanQuery)) 2
            else 1
        }
    }

    fun getAllCategories(): List<Category> = Category.values().toList()
}
