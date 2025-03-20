package fr.uge.android.watchoid.games.seb

data class Move(val from: Pair<Int, Int>, val to: Pair<Int, Int>, val piece: ChessPiece)

class ChessBoard {
    val board: Array<Array<ChessPiece?>> = Array(8) { arrayOfNulls<ChessPiece?>(8) }
    var lastMove: Move? = null
    var isWhiteTurn = true

    init {
        setupBoard()
    }

    fun movePiece(selectedPiece: ChessPiece, pair: Pair<Int, Int>) {
        // Sauvegarder l'état du plateau
        val previousBoard = board.map { it.copyOf() }
        val previousLastMove = lastMove
        val previousPosition = selectedPiece.position

        // Vérifiez si le mouvement est une prise en passant
        if (selectedPiece.type == PieceType.PAWN && lastMove != null) {
            val (lastFromRow, lastFromCol) = lastMove!!.from
            val (lastToRow, lastToCol) = lastMove!!.to
            val direction = if (selectedPiece.color == PieceColor.WHITE) 1 else -1
            if (lastMove!!.piece.type == PieceType.PAWN && lastMove!!.piece.color != selectedPiece.color &&
                lastFromRow == (if (selectedPiece.color == PieceColor.WHITE) 6 else 1) &&
                lastToRow == selectedPiece.position.first &&
                Math.abs(lastToCol - selectedPiece.position.second) == 1 &&
                pair == Pair(selectedPiece.position.first + direction, lastToCol)) {
                board[lastToRow][lastToCol] = null
            }
        }

        val attackedPiece = board[pair.first][pair.second]

        board[selectedPiece.position.first][selectedPiece.position.second] = null
        board[pair.first][pair.second] = selectedPiece
        selectedPiece.position = pair

        // Vérifiez si le roi est en echec après le coup
        if (isCheck()) {
            for (i in board.indices) {
                board[i] = previousBoard[i]
            }
            lastMove = previousLastMove
            selectedPiece.position = previousPosition
            board[previousPosition.first][previousPosition.second] = selectedPiece
            if (attackedPiece != null) {
                board[pair.first][pair.second] = attackedPiece
            }
        } else {
            lastMove = Move(previousPosition, pair, selectedPiece)
            isWhiteTurn = !isWhiteTurn
        }
    }

    fun isCheck(): Boolean {
        val color = if (isWhiteTurn) PieceColor.WHITE else PieceColor.BLACK
        val king = board.flatten().find { it?.type == PieceType.KING && it.color == color }!!
        val opponentColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val opponentMoves = board.flatten().filter { it?.color == opponentColor }.flatMap { getPossibleMoves(it!!, this, false) }
        return opponentMoves.contains(king.position)
    }

    fun isCheckmate(): Boolean {
        if (!isCheck()) return false
        val color = if (isWhiteTurn) PieceColor.WHITE else PieceColor.BLACK
        val king = board.flatten().find { it?.type == PieceType.KING && it.color == color }!!
        val kingMoves = getPossibleMoves(king, this)
        return kingMoves.isEmpty()
    }

    private fun setupBoard() {
        // pawn
        for (i in 0..7) {
            board[1][i] = ChessPiece(PieceType.PAWN, PieceColor.WHITE, Pair(1, i))
            board[6][i] = ChessPiece(PieceType.PAWN, PieceColor.BLACK, Pair(6, i))
        }

        // rook
        board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE, Pair(0, 0))
        board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE, Pair(0, 7))
        board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK, Pair(7, 0))
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK, Pair(7, 7))

        // night
        board[0][1] = ChessPiece(PieceType.NIGHT, PieceColor.WHITE, Pair(0, 1))
        board[0][6] = ChessPiece(PieceType.NIGHT, PieceColor.WHITE, Pair(0, 6))
        board[7][1] = ChessPiece(PieceType.NIGHT, PieceColor.BLACK, Pair(7, 1))
        board[7][6] = ChessPiece(PieceType.NIGHT, PieceColor.BLACK, Pair(7, 6))

        // bishop
        board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE, Pair(0, 2))
        board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE, Pair(0, 5))
        board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK, Pair(7, 2))
        board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK, Pair(7, 5))

        // queen
        board[0][4] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE, Pair(0, 4))
        board[7][4] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK, Pair(7, 4))

        // king
        board[0][3] = ChessPiece(PieceType.KING, PieceColor.WHITE, Pair(0, 3))
        board[7][3] = ChessPiece(PieceType.KING, PieceColor.BLACK, Pair(7, 3))
    }
}