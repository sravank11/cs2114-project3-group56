# Order Rush — Project 3 Deliverable 4

## How to Run

Import this folder into Eclipse using **File > Import > General > Existing Projects into Workspace**. Make sure `src` is a source folder and the course CS2 Support / `student.TestCase` library is configured. Run `src/app/Main.java` as a Java Application. The Swing window shows the customer line, food tray, score and strikes. Click **Add VIP Customer** to add a customer to the priority lane. VIP customers appear first and must be served before regular customers. Food, serving, free serving, refunds, and timers still work through the controller.

For model tests, right-click `src/model/LinkedQueueTest.java`, `GameTest.java`, `VipQueueTest.java`, `BadInputTest.java`, or another `*Test.java` file and choose **Run As > JUnit Test**. Tests run independently of the window. `GameControllerTest` tests controller-to-model interactions without opening a GUI. No server or database is required.

## Reflection

**How the design absorbed the change:** Deliverable 3 proposed a second VIP priority queue. We added `vipLine` and VIP membership tracking to `Game`, plus `spawnVipCustomer()` and `isVip()`. `getCustomers()` now returns VIPs before regular customers, while `getFrontCustomer()`, `serveOrder()`, `serveFree()`, `removeImpatientCustomer()`, `tick()`, `findCustomer()` and `endGame()` handle both queues. `GameController.vipClicked()` connects the new button to the model, and `GameWindow` displays VIP labels and the new control. The existing `Customer` class did not need to change: VIP membership is stored by `Game` rather than changing the customer object. That differs from the D3 prediction, which expected `Customer`, `Game`, and `GameWindow` to change. `GameController` also needed a small method, which the prediction did not mention.

**Model/view boundary:** The model still has no Swing dependency. The view reads the model's VIP status and customer ordering, but the model does not know about the GUI. The controller handles the VIP button click. The view does not maintain a separate VIP queue.

**Changes from D3:** The VIP lane is now implemented as a distinct queue, not just a label or score bonus. A manual **Add VIP Customer** button makes the new feature easy to demonstrate. High-score saving and difficulty selection are not part of this prototype. The existing rejection results and patience rules remain in place.

**What does not work yet:** High-score persistence, difficulty selection, and automatic/random VIP arrivals are not implemented. The VIP tests cover priority ordering and game-over behavior, and BadInputTest covers the eight scope bad-input scenarios plus three related edge cases. The entire suite must be run in Eclipse with the course test library. GUI behavior should also be checked manually on the lab computer.

**Making future changes cheaper:** A shared helper for iterating and removing customers across lanes would reduce duplication if another priority level were added.

## Next Steps

1. Run all JUnit tests with the course library and fix any failures; verify each scope bad-input case (Matthew Li and Matthew Lee).
2. Manually check the GUI, VIP ordering, patience, serving, refunds, restart, and game-over behavior (Sravan; all members review).
3. Add difficulty settings if time allows (Matthew Li). Add persistent high scores if time allows (Matthew Lee). VIP priority lane is the committed change feature (Sravan). Other unimplemented stretch goals are deferred.
4. Prepare the three-minute demo: launch Order Rush, add a regular customer, add a VIP, show the VIP move to the front, try serving the regular customer (rejected), serve the VIP, then show score and patience behavior (all members).

## Attribution

<table>
  <thead>
    <tr><th>Class or test file</th><th>Contributor</th><th>Work</th></tr>
  </thead>
  <tbody>
    <tr><td>Game.java</td><td>Matthew Li; Sravan (D4 VIP changes)</td><td>Original model and review; VIP queue logic added with GenAI assistance</td></tr>
    <tr><td>LinkedQueue.java</td><td>Matthew Li</td><td>Original queue implementation; GenAI-assisted JavaDocs</td></tr>
    <tr><td>Customer.java</td><td>Matthew Li</td><td>Original customer implementation; GenAI-assisted JavaDocs</td></tr>
    <tr><td>Order.java</td><td>Matthew Lee</td><td>Original implementation and review; GenAI-assisted JavaDocs</td></tr>
    <tr><td>MenuItem.java</td><td>Matthew Lee</td><td>Original implementation and review; GenAI-assisted JavaDocs</td></tr>
    <tr><td>CustomerState.java</td><td>Matthew Lee</td><td>Original implementation and review; GenAI-assisted JavaDocs</td></tr>
    <tr><td>ServeResult.java</td><td>Matthew Lee</td><td>Original implementation and review; GenAI-assisted JavaDocs</td></tr>
    <tr><td>GameListener.java</td><td>Sravan</td><td>Listener interface, GenAI-assisted</td></tr>
    <tr><td>GameWindow.java</td><td>Sravan</td><td>Swing GUI and VIP control, GenAI-assisted</td></tr>
    <tr><td>GameController.java</td><td>Sravan</td><td>Controller and VIP action, GenAI-assisted</td></tr>
    <tr><td>Main.java</td><td>Sravan</td><td>Entry point, GenAI-assisted</td></tr>
    <tr><td>GameTest.java</td><td>Matthew Li</td><td>Existing model tests</td></tr>
    <tr><td>LinkedQueueTest.java</td><td>Matthew Li</td><td>Existing queue tests</td></tr>
    <tr><td>CustomerTest.java</td><td>Matthew Li</td><td>Existing tests</td></tr>
    <tr><td>OrderTest.java</td><td>Matthew Lee</td><td>Existing tests</td></tr>
    <tr><td>MenuItemTest.java</td><td>Matthew Lee</td><td>Existing tests</td></tr>
    <tr><td>CustomerStateTest.java</td><td>Matthew Lee</td><td>Existing tests</td></tr>
    <tr><td>ServeResultTest.java</td><td>Matthew Lee</td><td>Existing tests</td></tr>
    <tr><td>GameControllerTest.java</td><td>Matthew Li</td><td>Controller-to-model tests, GenAI-assisted draft</td></tr>
    <tr><td>VipQueueTest.java</td><td>Sravan</td><td>New VIP normal/bad-input tests, GenAI-assisted</td></tr>
    <tr><td>BadInputTest.java</td><td>Sravan</td><td>Eleven scope bad-input and edge-case tests, GenAI-assisted</td></tr>
  </tbody>
</table>

**GenAI note:** ChatGPT was used to help implement and revise the GUI/controller code, the VIP priority feature, its test cases, and README wording. Existing teammates' attribution descriptions also indicate GenAI assistance with source and JavaDocs. The team must review generated changes and verify the attribution and tests before submitting.
