package gui;

import engine.Alliance;
import board.*;
import pieces.*;
import player.MoveTransition;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table {
    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final ScorePanel scorePanel;
    private Board chessBoard;
    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private JOptionPane optionPane;
    private final JSlider slider;
    private final static Dimension OUTER_FRAME_DIMENSION = new Dimension(1200,900);
    private final static Dimension BOARD_FRAME_DIMENSION = new Dimension(700,900);
    private final static Dimension SCORE_PANEL_DIMENSION = new Dimension(250,900);
    private final static Dimension TILE_PANEL_DIMENSION = new Dimension(50,50);
    private static String defaultPiecesImagePath = "icons/";
    static Timer timer;
    static int timeLeft, sliderTime;

    public Table() {
        this.gameFrame = new JFrame("ChessGame");
        this.gameFrame.setLayout(new BorderLayout());
        this.gameFrame.setJMenuBar(createTableMenuBar());
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.chessBoard = Board.createStandardBoard();
        this.slider = new JSlider(1,15,3);
        slider.setMajorTickSpacing(2);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        this.sliderTime = 3;
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                    optionPane.setInputValue(slider.getValue());
                sliderTime = (int) optionPane.getInputValue();

            }
        });
        JFrame parent = new JFrame();
        this.optionPane = new JOptionPane();
        optionPane.setMessage(new Object[] { "Select value for timer(mins): ", slider });
        JDialog dialog = optionPane.createDialog(parent, "Set timer");
        dialog.setVisible(true);
        this.timeLeft = this.sliderTime*60;
        this.boardPanel = new BoardPanel();
        this.scorePanel = new ScorePanel();
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.add(this.scorePanel, BorderLayout.EAST);
        this.gameFrame.setVisible(true);
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    private JMenuBar createTableMenuBar() {
        final JMenuBar tableMenuBar = new JMenuBar();
        tableMenuBar.add(createFileMenu());
        return tableMenuBar;
    }
    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem restart = new JMenuItem("Restart");
        restart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                gameFrame.dispose();
                new Table();
            }
        });
        fileMenu.add(restart);
        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private final class BoardPanel extends JPanel {
        final List<TilePanel> boardTiles;
        BoardPanel() {
            super(new GridLayout(8, 8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_FRAME_DIMENSION);
            validate();
        }
        public void drawBoard() {
            this.removeAll();
            this.boardTiles.clear();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            if(chessBoard.currentPlayer().isInCheck() || chessBoard.currentPlayer().isInCheckMate()){
                TilePanel tilePanel = boardTiles.get(chessBoard.currentPlayer().getPlayerKing().getPiecePosition());
                tilePanel.setBackground(Color.red);
            }
            validate();
        }
    }

    private final class TilePanel extends JPanel {
        private final int tileId;
        TilePanel(final BoardPanel boardPanel, final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon();
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (isRightMouseButton(e)) {
                        sourceTile = null;
                        destinationTile = null;
                        humanMovedPiece = null;
                        boardPanel.drawBoard();
                    }
                    else if (isLeftMouseButton(e)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getTile(tileId);
                            humanMovedPiece = sourceTile.getPiece();
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                            else {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        highlightLegals(chessBoard);
                                    }
                                });
                            }
                        }
                        else {
                            destinationTile = chessBoard.getTile(tileId);
                            final Move move = Move.createMove(chessBoard, sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());
                            if(move != Move.NULL_MOVE) {
                                final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                                if (transition.getMoveStatus().isDone()) {
                                    chessBoard = transition.getBoard();
                                    scorePanel.timeLabel.setText(chessBoard.currentPlayer().getAlliance().toString() + " to move");
                                    timeLeft = sliderTime*60;

                                    //add the attacked piece to respective panel
                                    if (move.isAttack()) {
                                        try {
                                            final BufferedImage image = ImageIO.read
                                                    (getClass().getResource(defaultPiecesImagePath + move.getAttackedPiece().getPieceAlliance().toString().substring(0, 1) +
                                                            move.getAttackedPiece().toString() + ".gif"));
                                            if (chessBoard.currentPlayer().getAlliance() == Alliance.BLACK) {
                                                scorePanel.black.add(new JLabel(new ImageIcon(image.getScaledInstance(50, 50, Image.SCALE_SMOOTH))));
                                            } else {
                                                scorePanel.white.add(new JLabel(new ImageIcon(image.getScaledInstance(50, 50, Image.SCALE_SMOOTH))));
                                            }
                                        } catch (Exception ae) {
                                            ae.printStackTrace();
                                        }
                                    }
                                    if(chessBoard.currentPlayer().isInCheckMate() || chessBoard.currentPlayer().isInStaleMate()) {
                                        if(chessBoard.currentPlayer().isInStaleMate()) {
                                            scorePanel.checkmateLabel.setText("<html>STALEMATE!<br>" + chessBoard.currentPlayer().getOpponent().getAlliance().toString() + " won the game</html>");
                                        }
                                        else {
                                            scorePanel.checkmateLabel.setText("<html>CHECKMATE!<br>" + chessBoard.currentPlayer().getOpponent().getAlliance().toString() + " won the game</html>");
                                        }
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                boardPanel.drawBoard();
                                            }
                                        });
                                        JFrame dialog = new JFrame();
                                        int a = JOptionPane.showConfirmDialog(dialog, "Click Yes to play again & No to quit");
                                        if (a == JOptionPane.YES_OPTION) {
                                            gameFrame.dispose();
                                            new Table();
                                        } else if(a == JOptionPane.NO_OPTION) {
                                            System.exit(0);
                                        }
                                    }
                                    else if(chessBoard.currentPlayer().isInCheck()) {
                                        scorePanel.checkmateLabel.setText("CHECK");
                                    }
                                    else {
                                        scorePanel.checkmateLabel.setText("");
                                    }
                                }
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                            destinationTile = null;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    boardPanel.drawBoard();
                                }
                            });
                        }
                    }
                }
                @Override
                public void mousePressed(final MouseEvent e) { }
                @Override
                public void mouseReleased(final MouseEvent e) { }
                @Override
                public void mouseEntered(final MouseEvent e) { }
                @Override
                public void mouseExited(final MouseEvent e) { }
            });
            validate();
        }
        private void assignTilePieceIcon() {
            this.removeAll();
            if (chessBoard.getTile(this.tileId).isTileOccupied()) {
                try {
                    final BufferedImage image = ImageIO.read
                            (getClass().getResource(defaultPiecesImagePath + chessBoard.getTile(this.tileId).getPiece().getPieceAlliance().toString().substring(0, 1) +
                                    chessBoard.getTile(this.tileId).getPiece().toString() + ".gif"));
                    add(new JLabel(new ImageIcon(image.getScaledInstance(50,50, Image.SCALE_SMOOTH))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        private void highlightLegals(final Board board) {
            if(true){
                for(final Move move : pieceLegalMoves(board)) {
                    TilePanel tilePanel = boardPanel.boardTiles.get(move.getDestinationCoordinate());
                    try {
                        tilePanel.setBorder(BorderFactory.createLineBorder(Color.blue,5));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()){
                this.setBorder(BorderFactory.createLineBorder(Color.red, 5));
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }
        private void assignTileColor() {
            Color darkTileColor = new Color(6725717);
            Color lightTileColor = new Color(14806216);
            if(BoardUtils.FIRST_ROW[this.tileId] ||
                BoardUtils.THIRD_ROW[this.tileId] ||
                BoardUtils.FIFTH_ROW[this.tileId] ||
                BoardUtils.SEVENTH_ROW[this.tileId]){
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            }
            else{
                setBackground(this.tileId % 2 == 0 ? darkTileColor : lightTileColor);
            }
        }
    }
    private final class ScorePanel extends JPanel {
        JPanel white, black, timePanel;
        JLabel timeLabel, checkmateLabel, time;
        ScorePanel() {
            super(new GridLayout(3,1));
            white = new JPanel(); black = new JPanel();
            white.setLayout(new FlowLayout()); black.setLayout(new FlowLayout());
            white.setBorder(BorderFactory.createTitledBorder(null, "WhitePlayer", TitledBorder.TOP,TitledBorder.CENTER, new Font("Calibri",Font.PLAIN,20), Color.BLUE));
            black.setBorder(BorderFactory.createTitledBorder(null, "BlackPlayer", TitledBorder.TOP,TitledBorder.CENTER, new Font("Calibri",Font.PLAIN,20), Color.BLUE));
            timePanel = new JPanel(new GridLayout(3,1));
            checkmateLabel = new JLabel("", SwingConstants.CENTER);
            checkmateLabel.setFont(new Font("Arial", Font.BOLD, 30));
            checkmateLabel.setForeground(Color.RED);
            timeLabel = new JLabel("WHITE to move: ", SwingConstants.CENTER);
            timeLabel.setFont(new Font("Calibri",Font.PLAIN,25));
            time = new JLabel("", JLabel.CENTER);
            time.setFont(new Font("Monaco",Font.PLAIN,35));
            time.setForeground(new Color(5773342));;
            timer=new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    timeLeft -= 1;
                    int min = timeLeft/60;
                    int sec = timeLeft%60;
                    time.setText(String.valueOf(min)+":"+(sec>=10?String.valueOf(sec):"0"+String.valueOf(sec)));
                    if(timeLeft<=0) {
                        timer.stop();
                        checkmateLabel.setText("<html>Time's Up!<br></html>");
                        JFrame dialog = new JFrame();
                        int a = JOptionPane.showConfirmDialog(dialog, "Want to play again?");
                        if(a == JOptionPane.YES_OPTION) {
                            gameFrame.dispose();
                            new Table();
                        }
                        else {
                            System.exit(0);
                        }
                    }
                }
            });
            timer.start();
            timePanel.add(timeLabel); timePanel.add(time); timePanel.add(checkmateLabel);
            add(white); add(black); add(timePanel);
            setPreferredSize(SCORE_PANEL_DIMENSION);
        }
    }

}
