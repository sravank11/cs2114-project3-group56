package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;
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
    private static final int CARD_W = 190;
    private static final int CARD_H = 170;
    private static final int GAP = 12;
    /** Holds the customer cards at animated positions (no layout manager). */
    private final JPanel cardRow = new JPanel(null)
    {
        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(GAP + live.size() * (CARD_W + GAP),
                CARD_H + 2 * GAP);
        }
    };
    private final Map<Integer, CustomerCard> cards = new HashMap<>();
    /** Cards for customers currently in the line, front first. */
    private final List<CustomerCard> live = new ArrayList<>();
    /** Cards still sliding or fading out after their customer left. */
    private final List<CustomerCard> leaving = new ArrayList<>();
    private final java.util.List<JButton> foodButtons = new java.util.ArrayList<>();
    private final JButton serveButton = styled(new JButton("Serve Order"), ACCENT);
    private final JButton freeButton = styled(new JButton("Serve Free"), GOOD);
    private final JButton refundButton = styled(new JButton("Refund"), BAD);
    private final JButton vipButton = styled(new JButton("Add VIP Customer"), GOOD);
    private final JButton clearButton = styled(new JButton("Clear Tray"), INK);
    private final JButton restartButton = styled(new JButton("Play Again"), ACCENT);
    // The only view state: which customer the player has picked
    private int selectedId = -1;
    // Animation-only memory, used to notice score and strike changes
    private int lastScore;
    private int lastStrikes;
    private final Effects effects = new Effects();
    private final Timer animator;
    private long lastFrameNs = System.nanoTime();

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
        setGlassPane(effects);
        effects.setVisible(true);
        lastScore = game.getScore();
        lastStrikes = game.getStrikes();
        game.addListener(this);
        refresh();
        animator = new Timer(16, event -> frame());
        animator.start();
    }

    @Override
    public void dispose()
    {
        animator.stop();
        super.dispose();
    }

    /** Advances every animation by the time since the last frame. */
    private void frame()
    {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrameNs) / 1e9);
        lastFrameNs = now;
        for (int i = 0; i < live.size(); i++)
        {
            live.get(i).animate(dt, GAP + i * (CARD_W + GAP));
        }
        for (CustomerCard card : new ArrayList<>(leaving))
        {
            card.animate(dt, -CARD_W * 2);
            if (card.isGone())
            {
                leaving.remove(card);
                cardRow.remove(card);
                cardRow.repaint();
            }
        }
        effects.tick(dt);
    }

    /** Starts a floating score text and a colored flash when the score or
     * strikes changed since the last refresh. */
    private void playResultEffects()
    {
        int score = game.getScore();
        int strikes = game.getStrikes();
        int x = cardRow.getParent() == null ? getWidth() / 2
            : SwingUtilities.convertPoint(cardRow.getParent(),
                cardRow.getParent().getWidth() / 2, 40, effects).x;
        int y = cardRow.getParent() == null ? 200
            : SwingUtilities.convertPoint(cardRow.getParent(), 0, 40,
                effects).y + 60;
        if (score > lastScore)
        {
            effects.floatText("+" + (score - lastScore), GOOD, x, y);
            effects.flash(GOOD, 0.18f);
        }
        else if (score < lastScore)
        {
            effects.floatText("-" + (lastScore - score), BAD, x, y);
        }
        if (strikes > lastStrikes)
        {
            effects.floatText("STRIKE!", BAD, x, y - 40);
            effects.flash(BAD, 0.35f);
        }
        lastScore = score;
        lastStrikes = strikes;
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
        vipButton.addActionListener(event -> controller.vipClicked());
        foods.add(vipButton);
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
        playResultEffects();
        List<Customer> customers = game.getCustomers();
        syncCards(customers);
        if (!cards.containsKey(selectedId))
        {
            selectedId = customers.isEmpty() ? -1 : customers.get(0).getId();
        }
        for (int i = 0; i < customers.size(); i++)
        {
            Customer c = customers.get(i);
            cards.get(c.getId()).update(c, i == 0, c.getId() == selectedId, game.isVip(c.getId()));
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
        vipButton.setEnabled(active);
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

    /** Creates cards for new customers (they slide in from the right) and
     * sends cards of departed customers off (they slide out to the left
     * unless they were served in time, in which case they fade out). */
    private void syncCards(List<Customer> customers)
    {
        boolean same = customers.size() == live.size();
        for (int i = 0; same && i < customers.size(); i++)
        {
            same = live.get(i) == cards.get(customers.get(i).getId());
        }
        if (same)
        {
            return;
        }
        Map<Integer, CustomerCard> next = new HashMap<>();
        List<CustomerCard> nextLive = new ArrayList<>();
        for (Customer c : customers)
        {
            CustomerCard card = cards.remove(c.getId());
            if (card == null)
            {
                final int id = c.getId();
                card = new CustomerCard(c);
                int startX = Math.max(getWidth(),
                    GAP + customers.size() * (CARD_W + GAP));
                card.setBounds(startX, GAP, CARD_W, CARD_H);
                card.setX(startX);
                card.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        selectedId = id;
                        refresh();
                    }
                });
                cardRow.add(card);
            }
            next.put(c.getId(), card);
            nextLive.add(card);
        }
        for (CustomerCard gone : cards.values())
        {
            gone.startLeaving();
            leaving.add(gone);
        }
        cards.clear();
        cards.putAll(next);
        live.clear();
        live.addAll(nextLive);
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
        private double shown = -1;
        private boolean impatient;
        private double pulse;
        private Color barColor = GOOD;

        private final Customer customer;
        private double x;
        private boolean leavingNow;
        private boolean fadeOut;
        private double fade = 1.0;

        CustomerCard(Customer customer)
        {
            this.customer = customer;
            setPreferredSize(new Dimension(CARD_W, CARD_H));
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

        void update(Customer c, boolean front, boolean selected, boolean vip)
        {
            title.setText((vip ? "VIP " : "") + "#" + c.getId() + (front ? "  🔔 AT WINDOW" : ""));
            StringBuilder lines = new StringBuilder("<html>");
            for (Map.Entry<MenuItem, Integer> entry
                : c.getOrder().getItems().entrySet())
            {
                // &nbsp; keeps the count on the same line as its emoji
                lines.append(emoji(entry.getKey())).append("&nbsp;x")
                    .append(entry.getValue()).append("<br>");
            }
            order.setText(lines.append("</html>").toString());
            impatient = c.getState() == CustomerState.IMPATIENT;
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
            if (shown < 0)
            {
                shown = fraction;
            }
            repaint();
        }

        void setX(double newX)
        {
            x = newX;
        }

        /** Called once when the customer has left the line. A customer
         * served in time fades out in place; anyone else slides left. */
        void startLeaving()
        {
            leavingNow = true;
            fadeOut = customer.getState() == CustomerState.SERVED
                && !impatient;
        }

        boolean isGone()
        {
            return fadeOut ? fade <= 0 : x <= -CARD_W;
        }

        /** Slides toward targetX, eases the bar toward the model's value
         * and pulses if impatient. */
        void animate(double dt, double targetX)
        {
            shown += (fraction - shown) * Math.min(1.0, dt * 10);
            pulse += dt * 6;
            if (leavingNow && fadeOut)
            {
                fade -= dt * 3;
            }
            else
            {
                x += (targetX - x) * Math.min(1.0, dt * 8);
            }
            setLocation((int) Math.round(x), GAP);
            repaint();
        }

        @Override
        public void paint(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) Math.max(0, Math.min(1, fade))));
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            if (impatient)
            {
                float alpha = (float) (0.12 + 0.12 * Math.sin(pulse));
                g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
                g2.setColor(BAD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
            }
            int w = getWidth() - 20;
            int y = getHeight() - 20;
            g2.setColor(new Color(0xE6DCCB));
            g2.fillRoundRect(10, y, w, 10, 10, 10);
            g2.setColor(barColor);
            g2.fillRoundRect(10, y, (int) (w * Math.max(0, shown)), 10, 10, 10);
            g2.dispose();
        }
    }

    /** Transparent overlay that draws floating texts and screen flashes.
     * It never takes mouse input. */
    private static class Effects extends JComponent
    {
        private static final double TEXT_SECONDS = 1.0;
        private static final double FLASH_SECONDS = 0.4;

        private static class FloatingText
        {
            String text;
            Color color;
            int x;
            int y;
            double age;
        }

        private final List<FloatingText> texts = new ArrayList<>();
        private Color flashColor = BAD;
        private float flashPeak;
        private double flashAge = FLASH_SECONDS;

        Effects()
        {
            setOpaque(false);
        }

        void floatText(String text, Color color, int x, int y)
        {
            FloatingText t = new FloatingText();
            t.text = text;
            t.color = color;
            t.x = x;
            t.y = y;
            texts.add(t);
        }

        void flash(Color color, float peak)
        {
            flashColor = color;
            flashPeak = peak;
            flashAge = 0;
        }

        void tick(double dt)
        {
            boolean active = flashAge < FLASH_SECONDS || !texts.isEmpty();
            if (!active)
            {
                return;
            }
            flashAge += dt;
            for (FloatingText t : texts)
            {
                t.age += dt;
            }
            texts.removeIf(t -> t.age >= TEXT_SECONDS);
            repaint();
        }

        @Override
        public boolean contains(int x, int y)
        {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            if (flashAge < FLASH_SECONDS)
            {
                float a = flashPeak * (float) (1 - flashAge / FLASH_SECONDS);
                g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
                g2.setColor(flashColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
            for (FloatingText t : texts)
            {
                double p = t.age / TEXT_SECONDS;
                g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, (float) Math.max(0, 1 - p * p)));
                g2.setColor(t.color);
                int w = g2.getFontMetrics().stringWidth(t.text);
                g2.drawString(t.text, t.x - w / 2, t.y - (int) (70 * p));
            }
            g2.dispose();
        }
    }
}
