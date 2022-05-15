# Библиотека для обработки исключений, выбрасываемых контроллерами.

**Для включения библиотеки в свой проект, необходимо:**
- добавить зависимость на стартер с библиотекой:
```
  implementation "ru.vtb.acrq.lib:exception-handling-spring-boot-starter:$version"
```
- добавить в properties файл клиентского проекта:
```
  custom.exceptions.handling.enable=true
```

**Кастомные исключения для выбрасывания в промежуточных операциях:**
- NotFoundException
- ValidationException

**Кастомное исключение, для наследования его бизнес-исключениями:**
- BusinessException

Бизнес исключения создаются в приложении-клиенте, свое на каждую операцию,
выбрасываются последними, в блоке catch, который ловит все возможные исключения
в промежуточных операциях. В cause необходимо передавать объект пойманного исключения.
Логгирование происходит средствами бибилиотеки, поэтому дополнительно логгировать
выбрасываемое бизнес-исключение не надо
```
public VmCamundaControls calculateRequestDisplayedButtons(Integer requestId, String upn, String requestType) {
        try {
            ValidateHelper.validateNotEmpty(requestId, "requestId");
            RequestDto requestDto;
            if (requestType.equals(RequestType.AIS.name())) {
                requestDto = findAisRequestService.findById(requestId);
            } else {
                throw new ValidationException("Некорректный тип заявки: " + requestType);
            }
            return buttonManager.calculateDisplayedButtonsForRequest(upn, requestDto);
        } catch (Exception e) {
            throw new CalculateDisplayedButtonsException("Displayed buttons calculation error", e);
        }
    }
```
По умолчанию все исключения, кроме ValidationException, отдаются на фронт с http 
статусом 500 и стандартным сообщением:
```
{
    "uuid": "de02e3fd-4927-432f-ac2b-81063cdaa070",
    "message": "Произошла системная ошибка. Обратитесь в службу поддержки. Код ошибки: de02e3fd-4927-432f-ac2b-81063cdaa070.",
    "httpStatus": "INTERNAL_SERVER_ERROR",
    "path": "/api/v1.0/button/",
    "timestamp": "2022-01-30T10:43:46.61866Z"
}
```
ValidationException отдается со статусом 400 и сообщением, взятым из исключения:
```
{
    "uuid": "ab9749e4-dce6-4ff9-bb5c-847b1a8970e0",
    "message": "Ошибка валидации данных. Получено пустое значение 'requestId'. Значение 'requestId' не может быть пустым",
    "httpStatus": "BAD_REQUEST",
    "path": "/api/v1.0/button/",
    "timestamp": "2022-01-30T11:12:11.581353Z"
}
```
Для изменения отдаваемого на фронт http статуса для какого-либо исключения, необходимо
добавить в properties клиентского проекта запись следующего вида:
```
custom.exceptions.handling.exception-to-http-status-map.java.lang.IllegalArgumentException=400
custom.exceptions.handling.exception-to-http-status-map.NotFoundException=400
```
Левая часть выражения - это property_name.map_key, правая часть выражения - значение
для указанного ключа. Ключ - это полное имя класса-исключения, значение - http статус.

Для проброса сообщения из исключения определенного типа на фронт, необходимо добавить в properties 
клиентского проекта запись следующего вида:
```
custom.exceptions.handling.exceptions-with-message-forwarding=java.lang.IllegalArgumentException,\
  NotFoundException
```