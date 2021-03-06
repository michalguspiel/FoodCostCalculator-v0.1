package com.erdees.foodcostcalc.data.productIncludedInHalfProduct

import androidx.lifecycle.LiveData
import com.erdees.foodcostcalc.model.ProductIncludedInHalfProduct

class ProductIncludedInHalfProductRepository(private val productIncludedInHalfProductDao: ProductIncludedInHalfProductDao) {
    val readAllData : LiveData<List<ProductIncludedInHalfProduct>> = productIncludedInHalfProductDao.getAllProductIncludedInHalfProduct()

    val readAllDataNotAsc : LiveData<List<ProductIncludedInHalfProduct>> =
        productIncludedInHalfProductDao.getAllProductIncludedInHalfProductNotAsc()

    fun getCertainProductsIncluded(productId: Long)
    = productIncludedInHalfProductDao.getCertainProductsIncluded(productId)

    suspend fun addProductIncludedInHalfProduct(productIncludedInHalfProduct: ProductIncludedInHalfProduct)
    = productIncludedInHalfProductDao.addProductIncludedInHalfProduct(productIncludedInHalfProduct)

    suspend fun editProductIncludedInHalfProduct(productIncludedInHalfProduct: ProductIncludedInHalfProduct)
    = productIncludedInHalfProductDao.editProductIncludedInHalfProduct(productIncludedInHalfProduct)

    suspend fun deleteProductIncludedInHalfProduct(productIncludedInHalfProduct: ProductIncludedInHalfProduct)
    = productIncludedInHalfProductDao.deleteProductIncludedInHalfProduct(productIncludedInHalfProduct)

    fun getProductsIncludedFromHalfProduct(halfProductId: Long)
    = productIncludedInHalfProductDao.getProductsFromHalfProduct(halfProductId)



    companion object{
        @Volatile
        private var instance: ProductIncludedInHalfProductRepository? = null

        fun getInstance(productIncludedInHalfProductDao: ProductIncludedInHalfProductDao) =
            instance ?: synchronized(this){
                instance ?: ProductIncludedInHalfProductRepository(productIncludedInHalfProductDao).also { instance = it }
            }
    }
}