# 📐 Pro UML & Architecture Designer

**Pro UML & Architecture Designer** is an advanced vector graphics editor built from scratch in **JavaFX**. The application is designed for creating UML diagrams, architectural schematics, and flowcharts, offering a seamless user experience (UX).


![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)

---

## ✨ Technical Highlights & Core Features

This project was built to demonstrate advanced object-oriented design, custom 2D rendering, and complex algorithmic problem-solving without relying on heavy external graphics libraries.

### 🧠 Algorithmic Routing & Geometry
* **Dynamic Orthogonal Routing Engine:** Implemented a custom 90-degree pathfinding algorithm. The engine calculates midpoints and uses mathematical clamping to ensure lines never intersect with node bounding boxes (maintaining a precise 20px safe distance).
* **Euclidean Hit-Detection (Auto-Anchoring):** Bypassed standard, rigid JavaFX hitboxes by developing a custom radius-based detection system using `Math.hypot()`. This calculates the exact distance between the cursor and anchor points in real-time, ensuring fluid connection snapping.
* **Spatial Alignment Engine (Smart Guides):** Engineered a Figma-style alignment system. It dynamically calculates coordinate intersections across the entire canvas in real-time, projecting visual magnetic guides and highlighting target edges when shapes align on the X or Y axes.

### 🎨 Advanced JavaFX Rendering & Architecture
* **Custom UI Pipeline (`CellFactory` Injection):** Overrode default JavaFX UI controls to render live vector graphics directly inside dropdown menus. This demonstrates a deep understanding of the JavaFX rendering pipeline and custom node lifecycle.
* **Dynamic Scene Graph Theming:** Built a real-time CSS injection system to toggle a True Dark Mode. It traverses and updates the JavaFX Scene Graph properties and nested layouts on-the-fly without requiring an application restart.
* **Vector Graphics Math:** All UML connection heads (Inheritance, Aggregation, Crow Foot) are mathematically drawn using custom Polygons and coordinate translations, ensuring infinite scaling without pixelation.

---

## 🚀 Installation & Setup

### System Requirements
* **Java Development Kit (JDK):** Version 11 or higher.
* **JavaFX SDK:** Version 11 or higher.

### Steps to Run from IDE (IntelliJ IDEA / Eclipse)
1. Clone this repository using the command: git clone https://github.com/ClaudiuPerdevara/uml-designer.git
2. Open the project in your preferred IDE.
3. Ensure JavaFX is added to the Modulepath.
4. Add the following VM arguments at runtime (adjusting the path to your local SDK): --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
5. Run the main class containing the main() method.

---

## 🖱️ Shortcuts & Controls

| Action | Control | Description |
| :--- | :--- | :--- |
| **Pan (Move Canvas)** | Right-Click (or Middle-Click) + Drag | Navigate freely across the infinite canvas. |
| **Zoom In/Out** | Ctrl + Scroll | Zoom in or out of the workspace. |
| **Multiple Selection** | Left-Click in empty space + Drag | Draw a bounding box to select multiple shapes. |
| **Move Connection Text** | Click on the orange dot | Adjust the position of labels on the lines. |
| **Modify UML Route** | Click on L-Shape handles | The orange handle moves the elbow, the green ones slide the anchor points. |

---

## 🗺️ Roadmap (Upcoming Features)

- [ ] More shapes for more complex diagrams.
- [ ] Direct export of diagrams to .PNG format (without background/grid).
- [ ] Undo / Redo system support (Ctrl+Z / Ctrl+Y).

