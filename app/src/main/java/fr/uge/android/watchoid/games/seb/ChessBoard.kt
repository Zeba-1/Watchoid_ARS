package fr.uge.android.watchoid.games.seb

class ChessBoard {
    val board: Array<Array<ChessPiece?>> = Array(8) { arrayOfNulls<ChessPiece?>(8) }

    init {
        setupBoard()
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

    fun movePiece(selectedPiece: ChessPiece, pair: Pair<Int, Int>) {
        board[selectedPiece.position.first][selectedPiece.position.second] = null
        board[pair.first][pair.second] = selectedPiece
        selectedPiece.position = pair
    }
}