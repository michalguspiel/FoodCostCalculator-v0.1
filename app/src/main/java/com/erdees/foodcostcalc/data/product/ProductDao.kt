package com.erdees.foodcostcalc.data.product

import androidx.lifecycle.LiveData
import androidx.room.*
import com.erdees.foodcostcalc.model.Product

/** DATA ACCESS OBJECT */
@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProduct(product: Product)

    @Query("SELECT * from products ORDER BY product_name ASC")
    fun getProducts(): LiveData<List<Product>>

    @Update
    suspend fun editProduct(newProduct: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE productId = :id")
    fun getProduct(id: Long): LiveData<Product>
}