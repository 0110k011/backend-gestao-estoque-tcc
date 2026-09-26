package com.gestao.api.service;

import com.gestao.api.dto.categoria.CategoriaRequest;
import com.gestao.api.dto.categoria.CategoriaResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.model.Categoria;
import com.gestao.api.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivoTrueOrderByNomeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new SkuDuplicadoException("Já existe categoria com o nome: " + request.nome());
        }

        Categoria categoria = new Categoria();
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        categoria.setAtivo(true);

        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));

        if (!categoria.getNome().equalsIgnoreCase(request.nome())
                && categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new SkuDuplicadoException("Já existe categoria com o nome: " + request.nome());
        }

        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());

        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void inativar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getAtivo()
        );
    }
}