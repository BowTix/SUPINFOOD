package com.example.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.food.ui.theme.*
import coil.compose.AsyncImage
import com.example.food.model.Meal
import com.example.food.viewmodel.MealUiState
import com.example.food.viewmodel.MealViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MealViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val selectedMeal by viewModel.selectedMeal.collectAsState()

                    LaunchedEffect(Unit) {
                        delay(2000)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        if (selectedMeal == null) {
                            RecipeScreen(
                                viewModel = viewModel,
                                onMealClick = { meal -> viewModel.selectMeal(meal) }
                            )
                        } else {
                            DetailScreen(
                                meal = selectedMeal!!,
                                viewModel = viewModel,
                                onBack = { viewModel.clearSelection() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(viewModel: MealViewModel, onMealClick: (Meal) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isFrench by viewModel.isFrench.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "SUPINFOOD",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFrench) "FR" else "EN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isFrench,
                    onCheckedChange = { viewModel.toggleLanguage(it) },
                    modifier = Modifier.graphicsLayer(scaleX = 0.65f, scaleY = 0.65f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = searchText,
            onValueChange = {
                searchText = it
                viewModel.searchMeals(it)
            },
            placeholder = {
                Text(
                    if (isFrench) "Rechercher une recette..." else "Search for recipes...",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f))
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                OutlinedButton(
                    onClick = {
                        searchText = ""
                        viewModel.selectCategory(category.name)
                    },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    modifier = Modifier.shadow(4.dp, CircleShape, spotColor = LuxuryGold.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = category.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val currentState = state) {
                is MealUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)

                is MealUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFrench) "Une erreur est survenue" else "Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.searchMeals(searchText) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (isFrench) "Réessayer" else "Retry")
                        }
                    }
                }

                is MealUiState.Success -> {
                    if (currentState.meals.isEmpty()) {
                        Text(
                            text = if (isFrench) "Aucun résultat." else "No results found.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(currentState.meals) { meal ->
                                RecipeItem(meal = meal, onClick = { onMealClick(meal) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(meal: Meal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = meal.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.Black.copy(alpha = 0.05f))
                )
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "${meal.category ?: ""} • ${meal.area ?: ""}".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DetailScreen(meal: Meal, viewModel: MealViewModel, onBack: () -> Unit) {
    val isFrench by viewModel.isFrench.collectAsState()
    val translatedText by viewModel.translatedInstructions.collectAsState()
    val isDownloading by viewModel.isDownloadingModel.collectAsState()
    val translatedIngredientsList by viewModel.translatedIngredients.collectAsState()

    LaunchedEffect(isFrench, meal) {
        if (isFrench && translatedText == null) {
            viewModel.translateMeal(meal)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.height(350.dp).fillMaxWidth()) {
            AsyncImage(
                model = meal.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(20.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                    .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.padding(32.dp)) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${meal.category ?: "Inconnu"} — ${meal.area ?: "Inconnu"}".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.width(60.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isFrench) "Ingrédients" else "Ingredients",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))

            val originalIngredients = meal.getIngredients()

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(originalIngredients.size) { index ->
                    val (originalName, _) = originalIngredients[index]
                    var displayName = originalName
                    var displayMeasure = ""

                    if (isFrench && translatedIngredientsList != null && index < translatedIngredientsList!!.size) {
                        val fullString = translatedIngredientsList!![index]
                        if (fullString.contains("(") && fullString.endsWith(")")) {
                            displayName = fullString.substringBeforeLast(" (").trim()
                            displayMeasure = fullString.substringAfterLast(" (").removeSuffix(")").trim()
                        } else {
                            displayName = fullString
                        }
                    } else {
                        displayMeasure = originalIngredients[index].second
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(84.dp)
                    ) {
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(6.dp, CircleShape, spotColor = LuxuryGold.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = "https://www.themealdb.com/images/ingredients/${originalName}.png",
                                    contentDescription = originalName,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (displayMeasure.isNotBlank()) {
                            Text(
                                text = displayMeasure,
                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = if (isFrench) "Préparation" else "Instructions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (isFrench && isDownloading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Traduction en cours...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = if (isFrench) translatedText ?: "Chargement..." else meal.instructions ?: "No instructions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.supinfood_logo_reduit),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(140.dp)
                    .shadow(12.dp, CircleShape, spotColor = LuxuryGold.copy(alpha = 0.2f), clip = false)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "SUPINFOOD",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}