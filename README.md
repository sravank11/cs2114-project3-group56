# cs2114-project3-group56

## Reflection
- What changed from your specification? ServeResult gained two new values which are REJECTED_IMPATIENT for serving an impatient customer at the window the normal way, and REJECTED_NOT_IMPATIENT for serveFree on customer who is still waiting.
- Did the model really stand alone? Yes, because currently there is no view code in the model, nothing in model refers to view or controller. 
- What happened to your unknowns? We figured out how to run every customer's timer at once, removing an expired customer from the middle, and generating new customers while the player works.
- What doesn't work yet? The GUI we had planned isn't fully worked on yet so we still need to consider how to make that work with the current code.


## Next Steps
- What is left to finish the MVP? The MVP is generally finished
- Your change feature: Although we mentioned VIP customers in our stretch goals, for our change feature instead of having those VIPs in the regular queue, there could be a second priority queue. This would change Customer, Game, and GameWindow.
- Which stretch goals will you attempt? We will focus on creating a usable GUI, high scores, VIP customers, and adding different difficulties are going to be the stretch goals we pursue for now. Other ones we had listed may be attempted as well if we have time.
- What new tests will you need? Tests mainly for making sure the GUI works together with the game code, if the VIP customers actually give more points, and tests for if high scores save over different games.
- Who does what? We will all work on the GUI a bit, Matthew Li will work on the difficulties, Matthew Lee will work on the high scores, and Sravan will work on the VIP customers.


## Attribution
| Class & Tests  | Name | Did |
| ------------- | ------------- | ------------- |
| Game | Matthew Li | Drafted and wrote with GenAI Assistance, GenAI did JavaDocs |
| LinkedQueue | Matthew Li |  Drafted and wrote with GenAI Assistance, GenAI did JavaDocs |
| Customer | Matthew Li | Drafted and wrote with GenAI Assistance, GenAI did JavaDocs |
| Order | Matthew Lee |   |
| MenuItem | Matthew Lee |  |
| CustomerState | Matthew Lee |  |
| ServeResult | Mathew Lee |    |
| GameListener | Sravan |  |
| GameWindow | Sravan  |    |
| GameController | Sravan |     |
| Main | Sravan |     |


