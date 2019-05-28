var clientApi = {
    show: function (id, show) {
        var style = document.getElementById(id).style.display;
        if (show) {
            document.getElementById(id).style.display = '';
        } else {
            document.getElementById(id).style.display = "none"
        }
    },
    integrateWPP: function (data) {
        function getRedirectUrlToWpp(request, succ, fail) {
            const URL = '/api/register'
            $.ajax({
                url: URL,
                type: 'POST',
                data: request,
                contentType: 'application/json',
                dataType: 'text',
                success: function (response) {
                    succ(response)
                },
                error: function (err) {
                    console.log('Error while registering request', err)
                    fail(err)
                }
            })
        };

        function submitRecur(request) {
            $.ajax({
                url: '/api/recurPayment',
                type: 'POST',
                data: request,
                contentType: 'application/json',
                dataType: 'text',
                success: function (response) {
                    alert(JSON.stringify(JSON.parse(response),null,2))
                },
                error: function (err) {
                    console.log('Error while registering request', err)
                }
            })
        };

        if (data["mode"] === "wpp-seamless-submit") { //payment form is already initialized. no call to getRedirectUrlToWpp necessary
            WPP.seamlessSubmit({
                onSuccess: function (response) {
                    alert(response)
                    console.log("success")
                },
                onError: function (response) {
                    alert(response)
                    console.error(response)
                }
            })
        } else if (data["mode"] === "wpp-recur-submit") {
            submitRecur(JSON.stringify(data));
        } else {
            getRedirectUrlToWpp(JSON.stringify(data), function (resp) {
                var respObj = JSON.parse(resp)
                if (respObj['payment-redirect-url']) {
                    switch (data.mode) {
                        case 'wpp-embedded' :
                            WPP.embeddedPayUrl(respObj['payment-redirect-url'])
                            break;
                        case 'wpp-eps-embedded' :
                            WPP.embeddedPayUrl(respObj['payment-redirect-url'])
                            break;
                        case 'wpp-standalone' :
                            WPP.hostedPayUrl(respObj['payment-redirect-url'])
                            break
                        case 'wpp-seamless-render':
                            WPP.seamlessRender({
                                wrappingDivId: 'seamless-form-target',
                                url: respObj['payment-redirect-url'],
                                onSuccess: function () {
                                    console.log('Seamless form rendered.')
                                },
                                onError: function (err) {
                                    console.error(err)
                                }
                            })
                            break
                        default:
                            break
                    }
                } else {
                    console.error('No redirect-url find in response: ' + response)
                }
            }, function (err) {
                console.error('Payment registration failed: ' + err.status + ': ' + err.statusText + ' : ' + err.responseText)
            })
        }
    },
    pay: function (mode) {
        switch (mode) {
            case 'wpp-standalone':
                this.show("submit", false)
                this.integrateWPP({mode: 'wpp-standalone', "payment-method": 'creditcard'})
                break
            case 'wpp-embedded':
                this.show("submit", false)
                this.integrateWPP({mode: 'wpp-embedded', "payment-method": 'creditcard'})
                break;
            case 'wpp-eps-embedded':
                this.show("submit", false)
                this.integrateWPP({mode: 'wpp-eps-embedded', "payment-method": 'eps'})
                break;
            case 'wpp-seamless-render':
                this.show("submit", true)
                this.integrateWPP({mode: 'wpp-seamless-render', "payment-method": 'creditcard'})
                break;
            case 'wpp-seamless-submit':
                this.integrateWPP({mode: 'wpp-seamless-submit', "payment-method": 'creditcard'})
                break;
            case 'wpp-recur-submit':
                this.integrateWPP({mode: 'wpp-recur-submit', "payment-method": 'creditcard'})
                break;
        }
    }
}
