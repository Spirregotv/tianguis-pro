package com.martinalejandroarenasescalera.appdaniel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.martinalejandroarenasescalera.appdaniel.R;
import com.martinalejandroarenasescalera.appdaniel.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductAdapter – RecyclerView para el módulo de Administrador.
 * Muestra nombre, precio, stock y botones de Editar/Eliminar.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductActionListener {
        void onEdit(Product product);
        void onDelete(Product product);
    }

    private final List<Product> products = new ArrayList<>();
    private final OnProductActionListener listener;

    public ProductAdapter(OnProductActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> newList) {
        products.clear();
        if (newList != null) products.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_admin, parent, false);
        return new ProductViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() { return products.size(); }

    // ── ViewHolder ───────────────────────────────────────────────
    class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName, tvPrice, tvStock, tvCategory;
        private final View btnEdit, btnDelete;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvProductStock);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Product product) {
            Context ctx = itemView.getContext();

            tvName.setText(product.getName());
            tvPrice.setText(String.format("$%.2f", product.getPrice()));
            tvStock.setText(ctx.getString(R.string.stock_label, product.getStock()));
            tvCategory.setText(product.getCategory() != null ? product.getCategory() : "");

            // Color del stock según disponibilidad
            if (product.getStock() == 0) {
                tvStock.setTextColor(ctx.getColor(R.color.stock_empty));
            } else if (product.getStock() <= 5) {
                tvStock.setTextColor(ctx.getColor(R.color.stock_low));
            } else {
                tvStock.setTextColor(ctx.getColor(R.color.stock_ok));
            }

            // Cargar imagen con Glide
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(ctx)
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.ic_product_placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imgProduct);
            } else {
                imgProduct.setImageResource(R.drawable.ic_product_placeholder);
            }

            btnEdit.setOnClickListener(v -> listener.onEdit(product));
            btnDelete.setOnClickListener(v -> listener.onDelete(product));
        }
    }
}
