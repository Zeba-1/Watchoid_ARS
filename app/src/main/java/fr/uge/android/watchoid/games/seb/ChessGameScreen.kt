package fr.uge.android.watchoid.games.seb

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.watchoid.R

@Composable
fun ChessGameScreen() {
    val chessBoard = remember { ChessBoard() }
    var selectedPiece by remember { mutableStateOf<ChessPiece?>(null) }
    var checkMateColor by remember { mutableStateOf<PieceColor?>(null) }

    val blackSquare = Color(0xFFAD3838)
    val columnLabels = listOf("H", "G", "F", "E", "D", "C", "B", "A")
    val rowLabels = listOf("1", "2", "3", "4", "5", "6", "7", "8")
    var possibleMoves: List<Pair<Int, Int>>

    checkMateColor = if (chessBoard.isWhiteTurn) {
        if (chessBoard.isCheckmate()) PieceColor.WHITE else null
    } else {
        if (chessBoard.isCheckmate()) PieceColor.BLACK else null
    }

    if (selectedPiece != null) {
        possibleMoves = getPossibleMoves(selectedPiece!!, chessBoard)
    } else {
        possibleMoves = emptyList()
    }

    Column {
        for (row in 0..7) {
            Row {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center

                ) {
                    Text(text = rowLabels[row], fontSize = 16.sp, color = Color.Black)
                }

                for (col in 0..7) {
                    val piece = chessBoard.board[row][col]
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if ((row + col) % 2 == 0) Color.White else blackSquare)
                            .clickable {
                                if (checkMateColor != null) return@clickable
                                if (selectedPiece != null && possibleMoves.contains(Pair(row, col))) {
                                    chessBoard.movePiece(selectedPiece!!, Pair(row, col))
                                    selectedPiece = null
                                } else if (piece != null && piece.color == if (chessBoard.isWhiteTurn) PieceColor.WHITE else PieceColor.BLACK) {
                                    selectedPiece = piece
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            val imageRes = when (it.type) {
                                PieceType.KING -> if (it.color == PieceColor.WHITE) R.drawable.white_king else R.drawable.black_king
                                PieceType.QUEEN -> if (it.color == PieceColor.WHITE) R.drawable.white_queen else R.drawable.black_queen
                                PieceType.ROOK -> if (it.color == PieceColor.WHITE) R.drawable.white_rook else R.drawable.black_rook
                                PieceType.BISHOP -> if (it.color == PieceColor.WHITE) R.drawable.white_bishop else R.drawable.black_bishop
                                PieceType.NIGHT -> if (it.color == PieceColor.WHITE) R.drawable.white_night else R.drawable.black_night
                                PieceType.PAWN -> if (it.color == PieceColor.WHITE) R.drawable.white_pawn else R.drawable.black_pawn
                            }
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        if (possibleMoves.contains(Pair(row, col))) {
                            Canvas(modifier = Modifier.size(48.dp)) {
                                if (chessBoard.board[row][col] != null) {
                                    drawCircle(
                                        color = Color.Gray,
                                        radius = 20.dp.toPx(),
                                        center = this.center,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                } else {
                                    drawCircle(
                                        color = Color.Gray,
                                        radius = 10.dp.toPx(),
                                        center = this.center
                                    )
                                    }
                            }
                        }
                    }
                }
            }
        }

        // Bottom column labels
        Row {
            Spacer(modifier = Modifier.size(24.dp))
            columnLabels.forEach { label ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, fontSize = 16.sp, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }

        if (checkMateColor != null) {
            Text (
                text = "Checkmate! ${if (checkMateColor == PieceColor.WHITE) "Black" else "White"} wins",
                fontSize = 24.sp,
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}