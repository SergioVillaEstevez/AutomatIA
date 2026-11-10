# Módulos de la fase 2

La fase 2 añade consumo de IA real a través del backend, precios versionados y aplicación del límite mensual por empresa. n8n, documentos y Ollama siguen fuera de alcance.

## Proveedor OpenAI

**Problema.** Las empresas necesitan completar texto con un modelo, sin que el navegador vea la clave ni inventemos tokens o precios.

**Cómo funciona.** `POST /api/v1/company/ai/completions` resuelve la configuración (`ai_provider_configuration`, por defecto global `DEFAULT` → `openai` / `gpt-4.1-mini`), comprueba el límite mensual, llama a OpenAI solo si hay `OPENAI_API_KEY`, estima el coste con la tarifa vigente y escribe una fila en `ai_usage` (`SUCCESS`, `FAILED` o `BLOCKED`).

El orden importa: si el límite está agotado responde `402 AI_BUDGET_EXCEEDED` aunque falte la clave. Si hay margen y falta la clave responde `503 AI_NOT_CONFIGURED`.

**Componentes.** `AiService`, `OpenAIProvider`, `AiCostCalculator`, `CompanyAiController`, `AiProperties`.

**Prueba.** `AiBudgetHttpTest` crea una empresa con límite 0 € y espera el bloqueo. El resto de la suite no llama a OpenAI de verdad.

## Precios y tarifas

**Problema.** Los precios cambian; no pueden vivir hardcodeados en Java.

**Cómo funciona.** `ai_model_pricing` guarda USD por millón de tokens con ventana de vigencia y nota de fuente. La migración `V2` siembra `gpt-4.1-mini` y `gpt-4o-mini` con la tarifa pública consultada el 2026-11-10. `POST /api/v1/admin/ai/pricing` cierra la tarifa anterior del mismo modelo y abre una nueva. `GET /api/v1/admin/ai/pricing` lista el historial.

La conversión a euros usa `NEIVER_USD_TO_EUR` (por defecto 0,92). No es un tipo de cambio en vivo.

**Componentes.** `AiModelPricing`, `AdminAiController`, semilla en `V2__ai_usage_and_pricing.sql`.

## Consumo y paneles

**Problema.** Administrador general y empresa tienen que ver gasto real del mes, no ceros inventados.

**Cómo funciona.** `GET .../ai/usage` y `.../usage/summary` listan peticiones y resumen mensual. Los paneles de resumen (`/api/v1/admin/dashboard`, `/api/v1/company/dashboard`) incluyen el gasto del mes. La interfaz Angular muestra Consumo de IA, tarifas (solo admin) y una prueba de petición en el portal de empresa.

`CompanyResponse.aiBudgetEnforced` refleja si el servidor aplica el límite (`neiver.ai.budget-enforced`, por defecto verdadero).

**Prueba.** Arrancar con presupuesto 0 y comprobar el 402; con clave y presupuesto positivo, una petición correcta deja fila `SUCCESS` y sube el resumen.
