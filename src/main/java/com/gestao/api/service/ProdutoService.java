package com.gestao.api.service;

import com.gestao.api.dto.produto.ProdutoRequest;
import com.gestao.api.dto.produto.ProdutoResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.model.Categoria;
import com.gestao.api.model.Produto;
import com.gestao.api.repository.CategoriaRepository;
import com.gestao.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return produtoRepository.findByAtivoTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> buscarPorNome(String nome, Pageable pageable) {
        return produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nome, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        return produtoRepository.findByAtivoTrueAndCategoriaId(categoriaId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsBySku(request.sku())) {
            throw new SkuDuplicadoException("Já existe produto com SKU: " + request.sku());
        }

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + request.categoriaId()));

        validarCoerenciaPrecos(request.precoCusto(), request.precoVenda());

        Produto produto = new Produto();
        produto.setSku(request.sku());
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPrecoCusto(request.precoCusto());
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        produto.setQuantidadeEstoque(0);
        produto.setAtivo(true);
        produto.setCategoria(categoria);

        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));

        if (produtoRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new SkuDuplicadoException("Já existe outro produto com SKU: " + request.sku());
        }

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + request.categoriaId()));

        validarCoerenciaPrecos(request.precoCusto(), request.precoVenda());

        produto.setSku(request.sku());
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPrecoCusto(request.precoCusto());
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        produto.setCategoria(categoria);

        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public void inativar(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    private void validarCoerenciaPrecos(java.math.BigDecimal custo, java.math.BigDecimal venda) {
        if (venda.compareTo(custo) < 0) {
            throw new IllegalArgumentException("Preço de venda não pode ser inferior ao preço de custo");
        }
    }

    private ProdutoResponse toResponse(Produto produto) {
        boolean abaixoDoMinimo = produto.getQuantidadeEstoque() <= produto.getEstoqueMinimo();
        return new ProdutoResponse(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPrecoCusto(),
                produto.getPrecoVenda(),
                produto.getQuantidadeEstoque(),
                produto.getEstoqueMinimo(),
                produto.getAtivo(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome(),
                abaixoDoMinimo
        );
    }
}