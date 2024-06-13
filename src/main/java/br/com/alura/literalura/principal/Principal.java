package br.com.alura.literalura.principal;

import br.com.alura.literalura.model.Autor;
import br.com.alura.literalura.model.AutorDTO;
import br.com.alura.literalura.model.Livro;
import br.com.alura.literalura.model.LivroDTO;
import br.com.alura.literalura.repository.LivroRepository;
import br.com.alura.literalura.service.ConsumoAPI;
import br.com.alura.literalura.service.ConverteDados;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Principal {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConverteDados converteDados;

    private final Scanner leitura = new Scanner(System.in);

    public Principal(LivroRepository livroRepository, ConsumoAPI consumoAPI, ConverteDados converteDados) {
        this.livroRepository = livroRepository;
        this.consumoAPI = consumoAPI;
        this.converteDados = converteDados;
    }

    public void executar() {
        boolean running = true;
        while (running) {
            exibirMenu();
            var opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1 -> buscarLivrosPeloTitulo();
                case 2 -> listarLivrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarLivrosPorIdioma();
                case 0 -> {
                    System.out.println("Cerrando");
                    running = false;
                }
                default -> System.out.println("Opcion invalida");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("""
            
                                 Menu
                       1- Buscar Libros por titulo
                       2- Mostrar libros registrados
                       3- Listar autores registrados
                       4- Listar libros por determinado idioma
                       0- Salir
                       
                       
            """);
    }

    private void salvarLivros(List<Livro> livros) {
        livros.forEach(livroRepository::save);
    }


    private void buscarLivrosPeloTitulo() {
        String baseURL = "https://gutendex.com/books?search=";

        try {
            System.out.println("Digite el título del libro: ");
            String titulo = leitura.nextLine();
            String endereco = baseURL + titulo.replace(" ", "%20");
            System.out.println("URL da API: " + endereco);

            String jsonResponse = consumoAPI.obterDados(endereco);
            System.out.println("Respuesta de API: " + jsonResponse);

            if (jsonResponse.isEmpty()) {
                System.out.println("Respuesta de API está vacia.");
                return;
            }


            JsonNode rootNode = converteDados.getObjectMapper().readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isEmpty()) {
                System.out.println("No se encontró el libro.");
                return;
            }


            List<LivroDTO> livrosDTO = converteDados.getObjectMapper()
                    .readerForListOf(LivroDTO.class)
                    .readValue(resultsNode);


            List<Livro> livrosExistentes = livroRepository.findByTitulo(titulo);
            if (!livrosExistentes.isEmpty()) {
                System.out.println("Remobiendo libros");
                for (Livro livroExistente : livrosExistentes) {
                    livrosDTO.removeIf(livroDTO -> livroExistente.getTitulo().equals(livroDTO.titulo()));
                }
            }

            // guarda libros
            if (!livrosDTO.isEmpty()) {
                System.out.println("Guardando");
                List<Livro> novosLivros = livrosDTO.stream().map(Livro::new).collect(Collectors.toList());
                salvarLivros(novosLivros);
                System.out.println("Guardados");
            } else {
                System.out.println("Libros ya registrados");
            }


            if (!livrosDTO.isEmpty()) {
                System.out.println("Libros encontrados:");
                Set<String> titulosExibidos = new HashSet<>();
                for (LivroDTO livro : livrosDTO) {
                    if (!titulosExibidos.contains(livro.titulo())) {
                        System.out.println(livro);
                        titulosExibidos.add(livro.titulo());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error buscando libro: " + e.getMessage());
        }
    }



    private void listarLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("No libro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("No autor registrado.");
        } else {
            livros.stream()
                    .map(Livro::getAutor)
                    .distinct()
                    .forEach(autor -> System.out.println(autor.getAutor()));
        }
    }



    private void listarLivrosPorIdioma() {
        System.out.println("""
            Digite idioma :
            Ingles (en)
            Portugues (pt)
            Español (es)
            Frances (fr)
            Aleman (de)
            """);
        String idioma = leitura.nextLine();

        List<Livro> livros = livroRepository.findByIdioma(idioma);
        if (livros.isEmpty()) {
            System.out.println("Ningun libro encontrado con  el idioma especificado.");
        } else {
            livros.forEach(livro -> {
                String titulo = livro.getTitulo();
                String autor = livro.getAutor().getAutor();
                String idiomaLivro = livro.getIdioma();

                System.out.println("Título: " + titulo);
                System.out.println("Autor: " + autor);
                System.out.println("Idioma: " + idiomaLivro);
                System.out.println("----------------------------------------");
            });
        }
    }


}
