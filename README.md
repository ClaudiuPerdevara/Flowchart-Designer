# 📐 Pro UML & Architecture Designer

**Pro UML & Architecture Designer** is an advanced vector graphics editor built from scratch in **JavaFX**. The application is designed for creating UML diagrams, architectural schematics, and flowcharts, offering a seamless user experience (UX) comparable to commercial tools.

It's not just a simple drawing tool; it's a complex engine equipped with **Auto-Anchoring**, **Orthogonal Routing**, and **Smart Alignment Guides**.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)

---

## ✨ Key Features

### 🧠 Smart Routing & UML Connections
* **Orthogonal Lines (L-Shape):** 90-degree routing algorithm with 3 interactive handles for fine-tuned shape bypassing.
* **Auto-Anchoring 2.0:** Lines will never intersect shapes. They anchor intelligently to the closest edge, maintaining a safe distance (20px clamping) from the shape's hitboxes.
* **Complete UML Arrows:** Native support for Inheritance (Hollow Triangle), Realization, Aggregation, Composition, and Crow Foot (for databases).
* **Smart Text Tethers:** Multiplicities and connection texts can be dragged freely anywhere, but they are kept on a mathematical "leash" so they never get lost on the canvas.

### 🧲 UX & Smart Alignment
* **Smart Guides (Figma-style):** When moving a shape, alignment guides appear instantly across the screen, and the edge of the target shape you perfectly aligned with is highlighted (thick stroke).
* **Hover Hitboxes:** Forgiving connection edges. The 14px radius allows for fluid connection snapping only when hovering over the green anchor point, leaving the rest of the shape free for dragging.
* **Snap to Grid:** Classic 20x20px grid alignment for clean architecture, perfectly synchronized with free movement.

### 🎨 Modern UI & Customization
* **True Dark Mode:** Instant switching between Light and Dark themes, affecting the entire UI system (Canvas, Properties Panel, Scrollbars, Dropdowns).
* **Visual Dropdowns (CellFactories):** Selection menus for line styles and arrows don't just display plain text; they render live vector UML icons directly in the list.
* **Property Inspector:** Modify background colors, line thickness, font types, and UML class texts "on-the-fly".

---

## 🚀 Installation & Setup

### System Requirements
* **Java Development Kit (JDK):** Version 11 or higher.
* **JavaFX SDK:** Version 11 or higher.

### Steps to Run from IDE (IntelliJ IDEA / Eclipse)
1. Clone this repository using the command: git clone https://github.com/username/uml-designer.git
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

- [ ] Reintroduce buttons for Architecture and Network shapes (Server, Database, Router).
- [ ] Direct export of diagrams to .PNG format (without background/grid).
- [ ] Undo / Redo system support (Ctrl+Z / Ctrl+Y).

---
*Project developed by [Your Name]. Designed for Software Engineering, System Design, and Database Architecture.*
