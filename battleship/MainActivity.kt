package com.example.battleship

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Main colors used in the game
object GameColors {
    val background = Color(0xFF1E1E1E)
    val gridBackground = Color(0xFF252525)
    val primary = Color(0xFF00BCD4)
    val secondary = Color(0xFFFF9800)
    val accent = Color(0xFFE91E63)
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFF44336)
    val darkGray = Color(0xFF333333)
    val lightGray = Color(0xFF555555)
}

// Data models for the game
enum class ShipType {
    BATTLESHIP, CARRIER, CRUISER, SUBMARINE
}

enum class GameMode {
    NORMAL
}

enum class PlayerAction {
    ATTACK, FORTIFY
}

// Represents a cell on the game grid
data class GridCell(
    val x: Int,
    val y: Int,
    var hasShip: Boolean = false,
    var isAttacked: Boolean = false,
    var fortified: Boolean = false,
    var shipType: ShipType? = null
)

// Represents a player in the game
data class Player(
    val id: Int,
    var grid: List<GridCell>,
    var score: Int = 0,
    var isWinner: Boolean = false
)

// Game state management
class GameState {
    val gridSize = 8
    var players = listOf(
        Player(1, generateEmptyGrid()),
        Player(2, generateEmptyGrid())
    )
    var currentPlayerIndex by mutableStateOf(0)
    var currentAction by mutableStateOf(PlayerAction.ATTACK)
    var gameOver by mutableStateOf(false)
    var winner by mutableStateOf<Player?>(null)

    private fun generateEmptyGrid(): List<GridCell> {
        val grid = mutableListOf<GridCell>()
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                grid.add(GridCell(x, y))
            }
        }
        return grid
    }

    fun getCurrentPlayer() = players[currentPlayerIndex]

    fun getOpponentPlayer() = players[1 - currentPlayerIndex]

    fun switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex
    }

    fun placeShipsRandomly() {
        players.forEach { player ->
            placeShip(player, ShipType.BATTLESHIP, 5)
            placeShip(player, ShipType.CARRIER, 4)
            placeShip(player, ShipType.CRUISER, 3)
            placeShip(player, ShipType.SUBMARINE, 2)
        }
    }

    private fun placeShip(player: Player, shipType: ShipType, size: Int) {
        val horizontal = (0..1).random() == 0
        var validPlacement = false

        while (!validPlacement) {
            val startX = (0 until gridSize).random()
            val startY = (0 until gridSize).random()

            validPlacement = if (horizontal) {
                if (startX + size > gridSize) false
                else checkValidPlacement(player, startX, startY, size, true)
            } else {
                if (startY + size > gridSize) false
                else checkValidPlacement(player, startX, startY, size, false)
            }

            if (validPlacement) {
                for (i in 0 until size) {
                    val x = if (horizontal) startX + i else startX
                    val y = if (horizontal) startY else startY + i
                    val index = y * gridSize + x
                    player.grid[index].hasShip = true
                    player.grid[index].shipType = shipType
                }
            }
        }
    }

    private fun checkValidPlacement(player: Player, startX: Int, startY: Int, size: Int, horizontal: Boolean): Boolean {
        for (i in 0 until size) {
            val x = if (horizontal) startX + i else startX
            val y = if (horizontal) startY else startY + i
            val index = y * gridSize + x
            if (player.grid[index].hasShip) {
                return false
            }
        }
        return true
    }

    fun performAttack(x: Int, y: Int): Boolean {
        val opponent = getOpponentPlayer()
        val index = y * gridSize + x
        val cell = opponent.grid[index]

        if (cell.isAttacked) {
            return false
        }

        cell.isAttacked = true

        if (cell.hasShip && !cell.fortified) {
            // Check if all ships are destroyed
            val allShipsDestroyed = opponent.grid.none { it.hasShip && !it.isAttacked && !it.fortified }
            if (allShipsDestroyed) {
                gameOver = true
                getCurrentPlayer().isWinner = true
                winner = getCurrentPlayer()
            }
            return true
        }

        return false
    }

    fun fortifyCell(x: Int, y: Int): Boolean {
        val currentPlayer = getCurrentPlayer()
        val index = y * gridSize + x
        val cell = currentPlayer.grid[index]

        if (!cell.hasShip || cell.isAttacked || cell.fortified) {
            return false
        }

        cell.fortified = true
        return true
    }

    fun resetGame() {
        players = listOf(
            Player(1, generateEmptyGrid()),
            Player(2, generateEmptyGrid())
        )
        currentPlayerIndex = 0
        currentAction = PlayerAction.ATTACK
        gameOver = false
        winner = null
        placeShipsRandomly()
    }
}

// Main app component
@Composable
fun BattleCommandApp() {
    val navController = rememberNavController()
    val gameState = remember { GameState() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("game") {
            gameState.resetGame()
            GameScreen(gameState = gameState, navController = navController)
        }
    }
}

// Home screen
@Composable
fun HomeScreen(navController: NavController) {
    var showRules by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BATTLE COMMAND",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Game logo/icon
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16213E))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(id = android.R.drawable.presence_online),
                    contentDescription = "Game Logo",
                    tint = Color.Red,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("game") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3460)),
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
            ) {
                Text("PLAY GAME", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showRules = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF533483)),
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
            ) {
                Text("GAME RULES", fontSize = 18.sp)
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { /* Settings functionality */ },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF16213E), CircleShape)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { /* Help functionality */ },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF16213E), CircleShape)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Help",
                    tint = Color.White
                )
            }
        }
    }

    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }
}

// Game rules dialog
@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "BATTLE COMMAND RULES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("• Each player starts with a fleet of ships placed on a grid.")
                Text("• The objective is to destroy your opponent's fleet before they destroy yours.")
                Text("• On your turn, choose between two actions:")
                Text("  - ATTACK: Target a cell on your opponent's grid to attack.")
                Text("  - FORTIFY: Reinforce one of your ships to protect it from a single attack.")
                Text("• The background color indicates whose turn it is.")
                Text("• Players take turns attacking or fortifying until one fleet is destroyed.")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("OK")
                }
            }
        }
    }
}

// Game screen
@Composable
fun GameScreen(gameState: GameState, navController: NavController) {
    val scope = rememberCoroutineScope()
    var showGameOverDialog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf("") }
    var showActionMessage by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.gameOver) {
        if (gameState.gameOver) {
            delay(500)
            showGameOverDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (gameState.currentPlayerIndex == 0) Color(0xFF16213E) else Color(0xFF533483)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game header
            Text(
                text = "BATTLE COMMAND",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Player ${gameState.currentPlayerIndex + 1}'s Turn",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Action toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    text = "ATTACK",
                    isSelected = gameState.currentAction == PlayerAction.ATTACK,
                    onClick = { gameState.currentAction = PlayerAction.ATTACK }
                )

                ActionButton(
                    text = "FORTIFY",
                    isSelected = gameState.currentAction == PlayerAction.FORTIFY,
                    onClick = { gameState.currentAction = PlayerAction.FORTIFY }
                )
            }

            // Game grid - opponent's grid when attacking, own grid when fortifying
            val gridToDisplay = if (gameState.currentAction == PlayerAction.ATTACK) {
                gameState.getOpponentPlayer().grid
            } else {
                gameState.getCurrentPlayer().grid
            }

            GameGrid(
                grid = gridToDisplay,
                isOpponentGrid = gameState.currentAction == PlayerAction.ATTACK,
                onCellClick = { x, y ->
                    when (gameState.currentAction) {
                        PlayerAction.ATTACK -> {
                            val success = gameState.performAttack(x, y)
                            if (success) {
                                actionMessage = "Hit!"
                                showActionMessage = true
                                if (!gameState.gameOver) {
                                    scope.launch {
                                        delay(1000)
                                        gameState.switchTurn()
                                    }
                                }
                            } else {
                                actionMessage = "Miss or already attacked!"
                                showActionMessage = true
                            }
                        }
                        PlayerAction.FORTIFY -> {
                            val success = gameState.fortifyCell(x, y)
                            if (success) {
                                actionMessage = "Ship fortified!"
                                showActionMessage = true
                                scope.launch {
                                    delay(1000)
                                    gameState.switchTurn()
                                }
                            } else {
                                actionMessage = "Invalid fortification target!"
                                showActionMessage = true
                            }
                        }
                    }
                }
            )

            if (showActionMessage) {
                LaunchedEffect(actionMessage) {
                    delay(2000)
                    showActionMessage = false
                }

                Text(
                    text = actionMessage,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (showGameOverDialog) {
        GameOverDialog(
            winner = gameState.winner?.id ?: 0,
            onPlayAgain = {
                gameState.resetGame()
                showGameOverDialog = false
            },
            onExit = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )
    }
}

// Action button composable
@Composable
fun ActionButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFE94560) else Color(0xFF533483)
        ),
        modifier = Modifier
            .width(120.dp)
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

// Game grid composable
@Composable
fun GameGrid(
    grid: List<GridCell>,
    isOpponentGrid: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    val gridSize = sqrt(grid.size.toFloat()).toInt()

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
    ) {
        items(grid.size) { index ->
            val cell = grid[index]
            val x = index % gridSize
            val y = index / gridSize

            GridCellItem(
                cell = cell,
                isOpponentGrid = isOpponentGrid,
                onClick = { onCellClick(x, y) }
            )
        }
    }
}

// Grid cell composable
@Composable
fun GridCellItem(
    cell: GridCell,
    isOpponentGrid: Boolean,
    onClick: () -> Unit
) {
    val cellColor = when {
        cell.isAttacked && cell.hasShip && !cell.fortified -> Color.Red
        cell.isAttacked -> Color(0xFF555555)
        cell.fortified -> Color(0xFF00FF00)
        cell.hasShip && !isOpponentGrid -> getShipTypeColor(cell.shipType)
        else -> Color(0xFF0F3460)
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(cellColor)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isAttacked && cell.hasShip && !cell.fortified) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Hit",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else if (cell.isAttacked) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

// Helper function for ship colors
fun getShipTypeColor(shipType: ShipType?): Color {
    return when (shipType) {
        ShipType.BATTLESHIP -> Color(0xFF3282B8)
        ShipType.CARRIER -> Color(0xFF1E5F74)
        ShipType.CRUISER -> Color(0xFF2D4059)
        ShipType.SUBMARINE -> Color(0xFF133B5C)
        null -> Color(0xFF0F3460)
    }
}

// Game over dialog
@Composable
fun GameOverDialog(
    winner: Int,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PLAYER $winner WINS!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFFE94560),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3460))
                    ) {
                        Text("PLAY AGAIN")
                    }

                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF533483))
                    ) {
                        Text("EXIT")
                    }
                }
            }
        }
    }
}

// Helper function for square root
fun sqrt(value: Float): Float = kotlin.math.sqrt(value)

// Main function for previewing the app
@Preview
@Composable
fun BattleCommandPreview() {
    BattleCommandApp()
}