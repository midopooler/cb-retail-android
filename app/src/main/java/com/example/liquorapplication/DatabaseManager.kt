package com.example.liquorapplication

import android.content.Context
import android.util.Log
import com.couchbase.lite.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseManager(private val context: Context) {
    private var database: Database? = null
    private val databaseName = "LiquorInventoryDB"
    private val collectionName = "liquor_items"
    
    init {
        openDatabase()
        setupIndexes() // Setup both basic and vector indexes
        seedSampleData()
    }
    
    private fun openDatabase() {
        try {
            CouchbaseLite.init(context)
            val config = DatabaseConfiguration()
            database = Database(databaseName, config)
            Log.d("DatabaseManager", "Database opened successfully")
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error opening database", e)
        }
    }
    
    private fun setupIndexes() {
        database?.let { db ->
            try {
                val collection = db.getCollection(collectionName) ?: db.createCollection(collectionName)
                
                // Create basic value indexes for search
                val nameIndex = IndexBuilder.valueIndex(ValueIndexItem.property("name"))
                collection.createIndex("name_index", nameIndex)
                
                val typeIndex = IndexBuilder.valueIndex(ValueIndexItem.property("type"))
                collection.createIndex("type_index", typeIndex)
                
                // Note: Vector search will be configured when available in the SDK
                Log.d("DatabaseManager", "Basic indexes and database setup completed")
                
                Log.d("DatabaseManager", "Indexes created successfully")
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Error creating indexes", e)
            }
        }
    }
    
    private fun seedSampleData() {
        val sampleLiquors = listOf(
            LiquorItem(name = "Johnnie Walker Black Label", type = "Whiskey", price = 45.99, imageURL = "whiskey1"),
            LiquorItem(name = "Grey Goose Vodka", type = "Vodka", price = 39.99, imageURL = "vodka1"),
            LiquorItem(name = "Bacardi Superior Rum", type = "Rum", price = 24.99, imageURL = "rum1"),
            LiquorItem(name = "Tanqueray Gin", type = "Gin", price = 29.99, imageURL = "gin1"),
            LiquorItem(name = "Patron Silver Tequila", type = "Tequila", price = 54.99, imageURL = "tequila1"),
            LiquorItem(name = "Hennessy VS Cognac", type = "Cognac", price = 49.99, imageURL = "cognac1"),
            LiquorItem(name = "Macallan 12 Year", type = "Whiskey", price = 79.99, imageURL = "whiskey2"),
            LiquorItem(name = "Belvedere Vodka", type = "Vodka", price = 44.99, imageURL = "vodka2"),
            LiquorItem(name = "Captain Morgan Spiced Rum", type = "Rum", price = 22.99, imageURL = "rum2"),
            LiquorItem(name = "Bombay Sapphire Gin", type = "Gin", price = 26.99, imageURL = "gin2"),
            LiquorItem(name = "Don Julio Blanco", type = "Tequila", price = 49.99, imageURL = "tequila2"),
            LiquorItem(name = "Remy Martin VSOP", type = "Cognac", price = 64.99, imageURL = "cognac2"),
            LiquorItem(name = "Jack Daniel's Old No. 7", type = "Whiskey", price = 29.99, imageURL = "whiskey3"),
            LiquorItem(name = "Absolut Original Vodka", type = "Vodka", price = 19.99, imageURL = "vodka3"),
            LiquorItem(name = "Mount Gay Eclipse Rum", type = "Rum", price = 27.99, imageURL = "rum3")
        )
        
        sampleLiquors.forEach { saveLiquorItem(it) }
    }
    
    fun saveLiquorItem(item: LiquorItem) {
        database?.let { db ->
            try {
                val collection = db.getCollection(collectionName) ?: db.createCollection(collectionName)
                val document = MutableDocument(item.id)
                
                document.setString("id", item.id) // Explicitly save the ID
                document.setString("name", item.name)
                document.setString("type", item.type)
                document.setDouble("price", item.price)
                document.setString("imageURL", item.imageURL)
                document.setInt("quantity", item.quantity)
                
                collection.save(document)
                Log.d("DatabaseManager", "Saved liquor item: ${item.name}")
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Error saving liquor item", e)
            }
        }
    }
    
    suspend fun getAllLiquorItems(): List<LiquorItem> = withContext(Dispatchers.IO) {
        database?.let { db ->
            try {
                val collection = db.getCollection(collectionName) ?: return@withContext emptyList()
                val query = QueryBuilder
                    .select(SelectResult.all())
                    .from(DataSource.collection(collection))
                
                val results = query.execute()
                val liquorItems = mutableListOf<LiquorItem>()
                
                results.forEach { result ->
                    val dict = result.getDictionary(collectionName)
                    dict?.let {
                        val id = it.getString("id") ?: return@let
                        val name = it.getString("name") ?: return@let
                        val type = it.getString("type") ?: return@let
                        val price = it.getDouble("price")
                        val imageURL = it.getString("imageURL") ?: return@let
                        val quantity = it.getInt("quantity")
                        
                        val item = LiquorItem(id, name, type, price, imageURL, quantity)
                        liquorItems.add(item)
                    }
                }
                
                Log.d("DatabaseManager", "Retrieved ${liquorItems.size} liquor items")
                return@withContext liquorItems
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Error fetching liquor items", e)
                return@withContext emptyList()
            }
        } ?: emptyList()
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int) {
        database?.let { db ->
            try {
                val collection = db.getCollection(collectionName) ?: return
                val document = collection.getDocument(itemId)
                document?.let {
                    val mutableDoc = it.toMutable()
                    mutableDoc.setInt("quantity", newQuantity)
                    collection.save(mutableDoc)
                    Log.d("DatabaseManager", "Updated quantity for $itemId to $newQuantity")
                }
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Error updating quantity", e)
            }
        }
    }
    
    suspend fun searchLiquor(searchText: String): List<LiquorItem> = withContext(Dispatchers.IO) {
        database?.let { db ->
            try {
                val collection = db.getCollection(collectionName) ?: return@withContext emptyList()
                
                Log.d("DatabaseManager", "Searching for: '$searchText'")
                
                // First, let's debug what data we actually have
                if (searchText.lowercase() == "debug") {
                    val allQuery = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.collection(collection))
                    val allResults = allQuery.execute()
                    allResults.forEach { result ->
                        val dict = result.getDictionary(0)
                        dict?.let {
                            Log.d("DatabaseManager", "DEBUG - Item: ${it.getString("name")} (${it.getString("type")})")
                        }
                    }
                }
                
                // Simplified search - match iOS exactly (case insensitive using uppercase comparison)
                val upperSearchText = searchText.uppercase()
                
                val query = QueryBuilder
                    .select(SelectResult.all())
                    .from(DataSource.collection(collection))
                
                val results = query.execute()
                val liquorItems = mutableListOf<LiquorItem>()
                val seenIds = mutableSetOf<String>() // Prevent duplicates
                
                results.forEach { result ->
                    val dict = result.getDictionary(0)
                    dict?.let {
                        val id = it.getString("id") ?: return@let
                        val name = it.getString("name") ?: return@let
                        val type = it.getString("type") ?: return@let
                        val price = it.getDouble("price")
                        val imageURL = it.getString("imageURL") ?: return@let
                        val quantity = it.getInt("quantity")
                        
                        // Filter in code to match search text (case insensitive)
                        val nameUpper = name.uppercase()
                        val typeUpper = type.uppercase()
                        
                        if ((nameUpper.contains(upperSearchText) || typeUpper.contains(upperSearchText)) && !seenIds.contains(id)) {
                            val item = LiquorItem(id, name, type, price, imageURL, quantity)
                            liquorItems.add(item)
                            seenIds.add(id)
                            
                            Log.d("DatabaseManager", "Found item: $name (searching for: $searchText)")
                        }
                    }
                }
                
                Log.d("DatabaseManager", "Search for '$searchText' returned ${liquorItems.size} items")
                return@withContext liquorItems
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Error searching liquor", e)
                return@withContext emptyList()
            }
        } ?: emptyList()
    }
} 