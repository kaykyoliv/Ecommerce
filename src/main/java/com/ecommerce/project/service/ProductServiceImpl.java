package com.ecommerce.project.service;

import com.ecommerce.project.dto.ProductDTO;
import com.ecommerce.project.dto.ProductResponse;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category","categoryId",categoryId));

        if (productRepository.existsByProductNameAndCategory(productDTO.getProductName(), category)) {
            throw new APIException("Product already exists in this category!");
        }

        Product product = modelMapper.map(productDTO, Product.class);

        product.setImage("default.png");
        product.setCategory(category);
        double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
        product.setSpecialPrice(specialPrice);
        Product savedProduct = productRepository.save(product);

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        if (products.isEmpty())
            throw new APIException("No Products Exist!!");
        Page<ProductDTO> productDTOS = products.map(p -> modelMapper.map(p, ProductDTO.class));
        return new ProductResponse(productDTOS);
    }

    @Override
    public ProductResponse getAllProductsByCategory(Pageable pageable, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category","categoryId",categoryId));

        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageable);
        Page<ProductDTO> productDTOPage = productPage.map(p -> modelMapper.map(p, ProductDTO.class));
        return new ProductResponse(productDTOPage);
    }

    @Override
    public ProductResponse getProductByKeyword(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%', pageable);
        Page<ProductDTO> productDTOS = productPage.map(p -> modelMapper.map(p, ProductDTO.class));
        return  new ProductResponse(productDTOS);
    }

    @Override
    public ProductDTO updateProduct(ProductDTO productDTO, Long productId) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        productFromDb.setProductName(productDTO.getProductName());
        productFromDb.setDescription(productDTO.getDescription());
        productFromDb.setQuantity(productDTO.getQuantity());
        productFromDb.setDiscount(productDTO.getDiscount());
        productFromDb.setPrice(productDTO.getPrice());
        productFromDb.setSpecialPrice(productDTO.getSpecialPrice());

        productFromDb = productRepository.save(productFromDb);
        return modelMapper.map(productFromDb, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        productRepository.delete(productFromDb);
        return modelMapper.map(productFromDb, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);

        productFromDb.setImage(fileName);

        productFromDb = productRepository.save(productFromDb);

        return modelMapper.map(productFromDb, ProductDTO.class);
    }

}