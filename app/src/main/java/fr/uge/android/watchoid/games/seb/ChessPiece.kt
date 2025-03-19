package fr.uge.android.watchoid.games.seb

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, NIGHT, PAWN
}

enum class PieceColor {
    WHITE, BLACK
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    var position: Pair<Int, Int>
)

fun getPossibleMoves(piece: ChessPiece, board: ChessBoard): List<Pair<Int, Int>> {
    val moves = mutableListOf<Pair<Int, Int>>()
    val (row, col) = piece.position

    when (piece.type) {
        PieceType.PAWN -> {
            val direction = if (piece.color == PieceColor.WHITE) 1 else -1
            val startRow = if (piece.color == PieceColor.WHITE) 1 else 6

            if (board.board[row + direction][col] == null) {
                moves.add(Pair(row + direction, col))
                // Two squares on first move
                if (row == startRow && board.board[row + 2 * direction][col] == null) {
                    moves.add(Pair(row + 2 * direction, col))
                }
            }
            // Capture
            if (col > 0 && board.board[row + direction][col - 1] != null && board.board[row + direction][col - 1]?.color != piece.color) {
                moves.add(Pair(row + direction, col - 1))
            }
            if (col < 7 && board.board[row + direction][col + 1] != null && board.board[row + direction][col + 1]?.color != piece.color) {
                moves.add(Pair(row + direction, col + 1))
            }
        }
        PieceType.ROOK -> {
            for (i in 1..7) {
                if (row + i < 8 && board.board[row + i][col]?.color != piece.color) {
                    moves.add(Pair(row + i, col))
                    if (board.board[row + i][col] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (row - i >= 0 && board.board[row - i][col]?.color != piece.color) {
                    moves.add(Pair(row - i, col))
                    if (board.board[row - i][col] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (col + i < 8 && board.board[row][col + i]?.color != piece.color) {
                    moves.add(Pair(row, col + i))
                    if (board.board[row][col + i] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (col - i >= 0 && board.board[row][col - i]?.color != piece.color) {
                    moves.add(Pair(row, col - i))
                    if (board.board[row][col - i] != null) break
                }
                else break
            }
        }
        PieceType.NIGHT -> {
            val knightMoves = listOf(
                Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1),
                Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2)
            )
            for (move in knightMoves) {
                val newRow = row + move.first
                val newCol = col + move.second
                if (newRow in 0..7 && newCol in 0..7 && board.board[newRow][newCol]?.color != piece.color) {
                    moves.add(Pair(newRow, newCol))
                }
            }
        }
        PieceType.BISHOP -> {
            for (i in 1..7) {
                if (row + i < 8 && col + i < 8 && board.board[row + i][col + i]?.color != piece.color) {
                    moves.add(Pair(row + i, col + i))
                    if (board.board[row + i][col + i] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (row + i < 8 && col - i >= 0 && board.board[row + i][col - i]?.color != piece.color) {
                    moves.add(Pair(row + i, col - i))
                    if (board.board[row + i][col - i] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (row - i >= 0 && col + i < 8 && board.board[row - i][col + i]?.color != piece.color) {
                    moves.add(Pair(row - i, col + i))
                    if (board.board[row - i][col + i] != null) break
                }
                else break
            }
            for (i in 1..7) {
                if (row - i >= 0 && col - i >= 0 && board.board[row - i][col - i]?.color != piece.color) {
                    moves.add(Pair(row - i, col - i))
                    if (board.board[row - i][col - i] != null) break
                }
                else break
            }
        }
        PieceType.QUEEN -> {
            moves.addAll(getPossibleMoves(ChessPiece(PieceType.ROOK, piece.color, piece.position), board))
            moves.addAll(getPossibleMoves(ChessPiece(PieceType.BISHOP, piece.color, piece.position), board))
        }
        PieceType.KING -> {
            val kingMoves = listOf(
                Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
                Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
            )
            for (move in kingMoves) {
                val newRow = row + move.first
                val newCol = col + move.second
                if (newRow in 0..7 && newCol in 0..7 && board.board[newRow][newCol]?.color != piece.color) {
                    moves.add(Pair(newRow, newCol))
                }
            }
        }
    }
    return moves
}

fun isCheckmate(board: ChessBoard, color: PieceColor): Boolean {
    val king = board.board.flatten().find { it?.type == PieceType.KING && it.color == color }!!
    val kingMoves = getPossibleMoves(king, board)
    return kingMoves.isEmpty()
}