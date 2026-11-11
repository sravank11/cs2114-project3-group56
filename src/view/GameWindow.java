package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import controller.GameController;
import model.Customer;
import model.Game;
import model.GameListener;
import model.MenuItem;

/** Simple Swing display for Order Rush.
 * @author Sravan Surapaneni
 * @version November 2026
 */
public class GameWindow extends JFrame implements GameListener
{
    private final Game game;
    private final GameController controller;
    private final JLabel scoreLabel = new JLabel();
    private final JLabel trayLabel = new JLabel();
    private final JLabel orderLabel = new JLabel();
    private final JLabel messageLabel = new JLabel("Welcome to Order Rush!");
    private final DefaultListModel<String> customerRows = new DefaultListModel<>();
    private final JList<String> customerList = new JList<>(customerRows);
    private final JButton serveButton = new JButton("Serve Order");
    private final JButton freeButton = new JButton("Serve Free");
    private final JButton refundButton = new JButton("Refund / Remove");
    private final JButton clearButton = new JButton("Clear Tray");
    private final java.util.List<JButton> foodButtons = new java.util.ArrayList<>();
    // True while refresh() rebuilds the list, so the selection events it
    // causes don't disable the action buttons mid-click
    private boolean rebuildingList = false;

    public GameWindow(Game game, GameController controller)
    {
        super("Order Rush");
        if (game == null || controller == null)
        {
            throw new IllegalArgumentException("game and controller are required");
        }
        this.game = game;
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new GridLayout(3, 1, 4, 4));
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        top.add(scoreLabel);
        top.add(orderLabel);
        top.add(trayLabel);
        add(top, BorderLayout.NORTH);

        customerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !rebuildingList)
            {
                refreshSelection();
            }
        });
        JScrollPane scroll = new JScrollPane(customerList);
        scroll.setBorder(BorderFactory.createTitledBorder("Customer line (select a customer)"));
        add(scroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(3, 1, 4, 6));
        controls.setBorder(BorderFactory.createEmptyBorder(5, 12, 12, 12));
        JPanel foods = new JPanel(new FlowLayout());
        for (MenuItem item : MenuItem.values())
        {
            JButton button = new JButton("Add " + item.name());
            button.addActionListener(event -> controller.foodClicked(item));
            foodButtons.add(button);
            foods.add(button);
        }
        clearButton.addActionListener(event -> controller.clearClicked());
        foods.add(clearButton);
        controls.add(foods);

        JPanel actions = new JPanel(new FlowLayout());
        serveButton.addActionListener(event -> {
            Customer selected = selectedCustomer();
            if (selected != null)
            {
                controller.serveClicked(selected.getId());
            }
        });
        freeButton.addActionListener(event -> {
            Customer selected = selectedCustomer();
            if (selected != null)
            {
                controller.serveFreeClicked(selected.getId());
            }
        });
        refundButton.addActionListener(event -> {
            Customer selected = selectedCustomer();
            if (selected != null)
            {
                if (!controller.refundClicked(selected.getId()))
                {
                    messageLabel.setText("Only impatient customers can be refunded.");
                }
            }
        });
        actions.add(serveButton);
        actions.add(freeButton);
        actions.add(refundButton);
        controls.add(actions);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controls.add(messageLabel);
        add(controls, BorderLayout.SOUTH);
        game.addListener(this);
        refresh();
    }

    private Customer selectedCustomer()
    {
        int index = customerList.getSelectedIndex();
        java.util.List<Customer> customers = game.getCustomers();
        return index >= 0 && index < customers.size() ? customers.get(index) : null;
    }

    private void refreshSelection()
    {
        Customer selected = selectedCustomer();
        if (selected == null)
        {
            orderLabel.setText("Selected order: none");
        }
        else
        {
            orderLabel.setText("Customer #" + selected.getId() + " order: "
                + formatItems(selected.getOrder().getItems()));
        }
        boolean enabled = selected != null && !game.isGameOver();
        serveButton.setEnabled(enabled);
        freeButton.setEnabled(enabled);
        refundButton.setEnabled(enabled);
    }

    private String formatItems(Map<MenuItem, Integer> items)
    {
        if (items.isEmpty())
        {
            return "empty";
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<MenuItem, Integer> entry : items.entrySet())
        {
            if (text.length() > 0)
            {
                text.append(", ");
            }
            text.append(entry.getKey()).append(" x").append(entry.getValue());
        }
        return text.toString();
    }

    private void refresh()
    {
        Customer previous = selectedCustomer();
        int previousId = previous == null ? -1 : previous.getId();
        rebuildingList = true;
        try
        {
            customerRows.clear();
            int indexToSelect = -1;
            java.util.List<Customer> customers = game.getCustomers();
            for (int i = 0; i < customers.size(); i++)
            {
                Customer customer = customers.get(i);
                customerRows.addElement("#" + customer.getId() + "  "
                    + customer.getState() + "  "
                    + String.format("%.1f", customer.getPatienceMs() / 1000.0)
                    + "s remaining" + (i == 0 ? "  [FRONT]" : ""));
                if (customer.getId() == previousId)
                {
                    indexToSelect = i;
                }
            }
            if (indexToSelect < 0 && !customers.isEmpty())
            {
                indexToSelect = 0;
            }
            if (indexToSelect >= 0)
            {
                customerList.setSelectedIndex(indexToSelect);
            }
        }
        finally
        {
            rebuildingList = false;
        }
        scoreLabel.setText("Score: " + game.getScore() + "    Strikes: "
            + game.getStrikes() + "/" + Game.MAX_STRIKES
            + (game.isGameOver() ? "     GAME OVER" : ""));
        trayLabel.setText("Tray: " + formatItems(game.getTray()));
        boolean active = !game.isGameOver();
        for (JButton button : foodButtons)
        {
            button.setEnabled(active);
        }
        clearButton.setEnabled(active);
        refreshSelection();
    }

    @Override
    public void gameChanged(String message)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> gameChanged(message));
            return;
        }
        if (message != null && !message.isEmpty())
        {
            messageLabel.setText(message);
        }
        refresh();
    }
}
