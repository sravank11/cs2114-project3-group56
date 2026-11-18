package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import controller.GameController;
import model.Customer;
import model.CustomerState;
import model.Game;
import model.GameListener;
import model.MenuItem;

/** Swing display for Order Rush. It keeps no game state of its own: every
 * refresh re-reads the Game, and every click goes through the controller.
 * The only view state is which customer card is selected.
 * @author Sravan Surapaneni
 * @version November 2026
 */
public class GameWindow extends JFrame implements GameListener
{
    private static final Color BG = new Color(0xFFF4E0);
    private static final Color PANEL = new Color(0xFFFFFF);
    private static final Color INK = new Color(0x3A2A1E);
    private static final Color ACCENT = new Color(0xD9531E);
    private static final Color GOOD = new Color(0x3FA34D);
    private static final Color WARN = new Color(0xE8A317);
    private static final Color BAD = new Color(0xC8372D);
    private static final Font BIG = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    private static final Font MID = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private final Game game;
    private final GameController controller;
    private final Runnable onRestart;

    private final JLabel scoreLabel = new JLabel();
    private final JLabel strikesLabel = new JLabel();
    private final JLabel trayLabel = new JLabel();
    private final JLabel messageLabel = new JLabel("Welcome to Order Rush!");
    private final JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
    private final Map<Integer, CustomerCard> cards = new HashMap<>();
    private final java.util.List<JButton> foodButtons = new java.util.ArrayList<>();
    private final JButton serveButton = styled(new JButton("Serve Order"), ACCENT);
    private final JButton freeButton = styled(new JButton("Serve Free"), GOOD);
    private final JButton refundButton = styled(new JButton("Refund"), BAD);
    private final JButton clearButton = styled(new JButton("Clear Tray"), INK);
    private final JButton restartButton = styled(new JButton("Play Again"), ACCENT);
    // The only view state: which customer the player has picked
    private int selectedId = -1;

    /**
     * Builds the window with no restart option.
     * @param game the model to display
     * @param controller receives the player's clicks
     */
    public GameWindow(Game game, GameController controller)
    {
        this(game, controller, null);
    }

    /**
     * Builds the window.
     * @param game the model to display
     * @param controller receives the player's clicks
     * @param onRestart run when the player clicks Play Again; may be null
     */
    public GameWindow(Game game, GameController controller, Runnable onRestart)
    {
        super("Order Rush");
        if (game == null || controller == null)
        {
            throw new IllegalArgumentException("game and controller are required");
        }
        this.game = game;
        this.controller = controller;
        this.onRestart = onRestart;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(700, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        cardRow.setBackground(BG);
        JScrollPane scroll = new JScrollPane(cardRow,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        JPanel middle = new JPanel(new BorderLayout());
        middle.setBackground(BG);
        JLabel lineTitle = new JLabel("  Customer line  (click a customer to select)");
        lineTitle.setFont(MID);
        lineTitle.setForeground(INK);
        middle.add(lineTitle, BorderLayout.NORTH);
        middle.add(scroll, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        add(buildControls(), BorderLayout.SOUTH);
        game.addListener(this);
        refresh();
    }

    private static JButton styled(JButton button, Color color)
    {
        button.setFont(MID);
        button.setForeground(INK);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setBackground(PANEL);
        return button;
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("🍔 ORDER RUSH");
        title.setFont(BIG);
        title.setForeground(Color.WHITE);
        scoreLabel.setFont(BIG);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        strikesLabel.setFont(BIG);
        strikesLabel.setForeground(Color.WHITE);
        restartButton.addActionListener(event -> {
            if (onRestart != null)
            {
                onRestart.run();
            }
        });
        restartButton.setVisible(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(strikesLabel);
        right.add(restartButton);
        header.add(title, BorderLayout.WEST);
        header.add(scoreLabel, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildControls()
    {
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(PANEL);
        controls.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, ACCENT),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        trayLabel.setFont(MID);
        trayLabel.setForeground(INK);
        trayLabel.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(trayLabel);
        controls.add(Box.createVerticalStrut(8));

        JPanel foods = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        foods.setOpaque(false);
        foods.setAlignmentX(LEFT_ALIGNMENT);
        for (MenuItem item : MenuItem.values())
        {
            JButton button = styled(new JButton(emoji(item) + " Add "
                + capitalize(item)), WARN);
            button.addActionListener(event -> controller.foodClicked(item));
            foodButtons.add(button);
            foods.add(button);
        }
        clearButton.addActionListener(event -> controller.clearClicked());
        foods.add(clearButton);
        controls.add(foods);
        controls.add(Box.createVerticalStrut(8));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        serveButton.addActionListener(event -> {
            if (selectedId >= 0)
            {
                controller.serveClicked(selectedId);
            }
        });
        freeButton.addActionListener(event -> {
            if (selectedId >= 0)
            {
                controller.serveFreeClicked(selectedId);
            }
        });
        refundButton.addActionListener(event -> {
            if (selectedId >= 0)
            {
                controller.refundClicked(selectedId);
            }
        });
        actions.add(serveButton);
        actions.add(freeButton);
        actions.add(refundButton);
        controls.add(actions);
        controls.add(Box.createVerticalStrut(8));

        messageLabel.setFont(SMALL);
        messageLabel.setForeground(INK);
        messageLabel.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(messageLabel);
        return controls;
    }

    private static String emoji(MenuItem item)
    {
        switch (item)
        {
            case BURGER:
                return "🍔";
            case FRIES:
                return "🍟";
            default:
                return "🥤";
        }
    }

    private static String capitalize(MenuItem item)
    {
        String name = item.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static String formatItems(Map<MenuItem, Integer> items)
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
                text.append("  ");
            }
            text.append(emoji(entry.getKey())).append(" x")
                .append(entry.getValue());
        }
        return text.toString();
    }

    /** Re-reads the whole game and updates every component. */
    private void refresh()
    {
        List<Customer> customers = game.getCustomers();
        syncCards(customers);
        if (!cards.containsKey(selectedId))
        {
            selectedId = customers.isEmpty() ? -1 : customers.get(0).getId();
        }
        for (int i = 0; i < customers.size(); i++)
        {
            Customer c = customers.get(i);
            cards.get(c.getId()).update(c, i == 0, c.getId() == selectedId);
        }

        scoreLabel.setText("Score: " + game.getScore());
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            hearts.append(i < game.getStrikes() ? "❌" : "⭕");
        }
        strikesLabel.setText(hearts.toString());
        trayLabel.setText("Tray:  " + formatItems(game.getTray()));

        boolean active = !game.isGameOver();
        for (JButton button : foodButtons)
        {
            button.setEnabled(active);
        }
        clearButton.setEnabled(active && !game.getTray().isEmpty());
        Customer selected = null;
        for (Customer c : customers)
        {
            if (c.getId() == selectedId)
            {
                selected = c;
            }
        }
        boolean hasSel = active && selected != null;
        boolean impatient = hasSel
            && selected.getState() == CustomerState.IMPATIENT;
        serveButton.setEnabled(hasSel && !impatient);
        freeButton.setEnabled(impatient);
        refundButton.setEnabled(impatient);
        restartButton.setVisible(game.isGameOver() && onRestart != null);
        if (game.isGameOver())
        {
            messageLabel.setForeground(BAD);
        }
        else
        {
            messageLabel.setForeground(INK);
        }
    }

    /** Adds and removes cards only when the line's membership or order
     * changed, so clicks aren't lost to constantly rebuilt components. */
    private void syncCards(List<Customer> customers)
    {
        boolean same = customers.size() == cardRow.getComponentCount();
        for (int i = 0; same && i < customers.size(); i++)
        {
            same = cardRow.getComponent(i) == cards.get(customers.get(i).getId());
        }
        if (same)
        {
            return;
        }
        cardRow.removeAll();
        Map<Integer, CustomerCard> next = new HashMap<>();
        for (Customer c : customers)
        {
            CustomerCard card = cards.get(c.getId());
            if (card == null)
            {
                final int id = c.getId();
                card = new CustomerCard(id);
                card.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        selectedId = id;
                        refresh();
                    }
                });
            }
            next.put(c.getId(), card);
            cardRow.add(card);
        }
        cards.clear();
        cards.putAll(next);
        cardRow.revalidate();
        cardRow.repaint();
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

    /** One customer in the line: order, state and a patience bar. */
    private static class CustomerCard extends JPanel
    {
        private final JLabel title = new JLabel();
        private final JLabel status = new JLabel();
        private final JLabel order = new JLabel();
        private double fraction = 1.0;
        private Color barColor = GOOD;

        CustomerCard(int id)
        {
            setPreferredSize(new Dimension(190, 170));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(PANEL);
            title.setFont(MID);
            status.setFont(SMALL);
            order.setFont(MID);
            title.setForeground(INK);
            status.setForeground(INK);
            order.setForeground(INK);
            add(title);
            add(Box.createVerticalStrut(4));
            add(order);
            add(Box.createVerticalStrut(4));
            add(status);
            add(Box.createVerticalGlue());
            setCursor(java.awt.Cursor.getPredefinedCursor(
                java.awt.Cursor.HAND_CURSOR));
        }

        void update(Customer c, boolean front, boolean selected)
        {
            title.setText("#" + c.getId() + (front ? "  🔔 AT WINDOW" : ""));
            StringBuilder lines = new StringBuilder("<html>");
            for (Map.Entry<MenuItem, Integer> entry
                : c.getOrder().getItems().entrySet())
            {
                // &nbsp; keeps the count on the same line as its emoji
                lines.append(emoji(entry.getKey())).append("&nbsp;x")
                    .append(entry.getValue()).append("<br>");
            }
            order.setText(lines.append("</html>").toString());
            boolean impatient = c.getState() == CustomerState.IMPATIENT;
            status.setText(impatient ? "IMPATIENT!" : String.format(
                "%.1fs left", c.getPatienceMs() / 1000.0));
            fraction = c.getMaxPatienceMs() <= 0 ? 0
                : Math.max(0, Math.min(1.0,
                    (double) c.getPatienceMs() / c.getMaxPatienceMs()));
            barColor = impatient || fraction < 0.25 ? BAD
                : fraction < 0.5 ? WARN : GOOD;
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    selected ? ACCENT : new Color(0xD8C8B0), selected ? 4 : 1),
                BorderFactory.createEmptyBorder(8, 10, 18, 10)));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 20;
            int y = getHeight() - 20;
            g2.setColor(new Color(0xE6DCCB));
            g2.fillRoundRect(10, y, w, 10, 10, 10);
            g2.setColor(barColor);
            g2.fillRoundRect(10, y, (int) (w * fraction), 10, 10, 10);
            g2.dispose();
        }
    }
}
