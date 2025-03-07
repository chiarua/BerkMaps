/**
 * Map project javascript file written for CS61B/CS61BL.
 * This is not an example of good javascript or programming practice.
 * Feel free to improve this front-end for your own personal pleasure.
 * Authors: Alan Yao (Spring 2016), Colby Guan (Spring 2017), Alexander Hwang (Spring 2018),
 *          Eli Lipsitz (Spring 2019), Yuanbo Han
 * If using, please credit authors.
 **/

$(function() {
    'use strict';

    /* ══════════════════════════════════ ೋღ PROPERTIES ღೋ ════════════════════════════════ */
    const $body = $('#mapbody');
    const $routeStatus = $('#status-route');
    const $loadingStatus = $('#status-loading');
    const $errorStatus = $('#status-error');
    const $warningsContainer = $('#status-warnings');
    const $directionsText = $('#directions-text');
    const themeableElements = ['body', '.actions', '.card', '.search', '.ui-autocomplete',
        '.status', '.settings', '.clear', '.action-icon', '.location-details'];
    const SAFE_WIDTH = 1120;
    const SAFE_HEIGHT = 800;
    // pseudo-lock
    let getInProgress = false;
    let updatePending = false;
    let route_params = {};
    let map;
    let dest;
    let markers = [];
    let host;
    let ullon_bound, ullat_bound, lrlon_bound, lrlat_bound;
    let img_w, img_h;
    let constrain, theme;

    const base_move_delta = 64;
    const MAX_LEVEL = 7;
    const MIN_LEVEL = 1; // Level limits based on pulled data
    const START_LAT = 37.871826;
    const START_LON = -122.260086;
    const START_DEPTH = 3;
    const ROOT_ULLAT = 37.892195547244356;
    const ROOT_ULLON = -122.2998046875;
    const ROOT_LRLAT = 37.82280243352756;
    const ROOT_LRLON = -122.2119140625;

    let w = $body.width();
    let h = $body.height();
    let depth = START_DEPTH;
    let lat = START_LAT;
    let lon = START_LON;

    /* Set server URIs */
    if (document.location.hostname !== 'localhost') {
        host = 'https://' + document.location.host;
    } else {
        host = 'http://' + document.location.host;
    }

    const raster_server = host + '/raster';
    const route_server = host + '/route';
    const clear_route = host + '/clear_route';
    const search = host + '/search';

    /* ══════════════════════════ ೋღ HELPERS ღೋ ══════════════════════════ */
    function get_londpp() { return (ROOT_LRLON - ROOT_ULLON) / 256 / Math.pow(2, depth); }

    function get_latdpp() { return (ROOT_LRLAT - ROOT_ULLAT) / 256 / Math.pow(2, depth); }

    function get_view_bounds() {
        return {
            ullon: lon - (0.5 * w * get_londpp()),
            lrlon: lon + (0.5 * w * get_londpp()),
            ullat: lat - (0.5 * h * get_latdpp()),
            lrlat: lat + (0.5 * h * get_latdpp())
        }
    }

    function removeMarkers() {
        for (let i = 0; i < markers.length; i++) {
            markers[i].element.remove();
        }
        markers = [];
    }

    function updateImg() {
        if (getInProgress) {
            updatePending = true;
            return;
        }

        $loadingStatus.show();
        getInProgress = true;
        const params = get_view_bounds();
        params.w = w;
        params.h = h;
        console.log(params);
        $warningsContainer.empty();
        $.get({
            async: true,
            url: raster_server,
            data: params,
            success: function(data) {
                console.log(data);
                if (data.query_success) {
                    $loadingStatus.hide();
                    map.src = 'data:image/png;base64,' + data.b64_encoded_image_data;
                    console.log('Updating map with image length: ' +
                        data.b64_encoded_image_data.length);
                    ullon_bound = data.raster_ul_lon;
                    ullat_bound = data.raster_ul_lat;
                    lrlon_bound = data.raster_lr_lon;
                    lrlat_bound = data.raster_lr_lat;
                    img_w = data.raster_width;
                    img_h = data.raster_height;
                    getInProgress = false;

                    const warnings = [];
                    if (data.depth !== depth) {
                        warnings.push("got depth " + data.depth + " but was expecting " + depth);
                    }
                    if (img_w > params.w + 512) {
                        warnings.push("got much wider image than expected. requested width: " + params.w + ". got: " + img_w);
                    }
                    if (img_h > params.h + 512) {
                        warnings.push("got much taller image than expected. requested height: " + params.h + ". got: " + img_h);
                    }
                    if (warnings.length > 0) {
                        const ele = $('<div/>', {
                            class: 'card-content'
                        });
                        ele.html("Warnings:<br>" + warnings.join("<br>"));
                        ele.appendTo($warningsContainer);
                    }

                    updateT();

                    if (updatePending) {
                        updatePending = false;
                        updateImg();
                    }
                } else {
                    $loadingStatus.hide();
                }
            },
            error: function() {
                getInProgress = false;
                $errorStatus.show();
                setTimeout(function() {
                    $errorStatus.fadeOut();
                }, 4000);
            },
            dataType: 'json'
        });
    }

    function updateT() {
        const londpp = get_londpp();
        const latdpp = get_latdpp();
        const computed = get_view_bounds();
        const tx = (ullon_bound - computed.ullon) / londpp;
        const ty = (ullat_bound - computed.ullat) / latdpp;

        const newHash = "lat=" + lat + "&lon=" + lon + "&depth=" + depth;
        history.replaceState(null, null, document.location.pathname + '#' + newHash);

        map.style.transform = 'translateX(' + tx + 'px) translateY(' + ty + 'px)';
        for (let i = 0; i < markers.length; i++) {
            const marker = markers[i];
            const marker_tx = (marker.lon - computed.ullon) / londpp;
            const marker_ty = (marker.lat - computed.ullat) / latdpp;
            marker.element.css('transform', 'translateX(' + marker_tx + 'px) translateY(' + marker_ty + 'px)');
        }
        // validate transform - true if img needs updating
        return computed.ullon < ullon_bound || computed.ullat > ullat_bound ||
            computed.lrlon > lrlon_bound || computed.lrlat < lrlat_bound;
    }

    function updateRoute() {
        $.get({
            async: true,
            url: route_server,
            data: route_params,
            success: function(data) {
                data = JSON.parse(data);
                updateImg();
                if (data.directions_success) {
                    $directionsText.html(data.directions);
                } else {
                    $directionsText.html('Double click on the map to set start and end points.');
                }
            },
        });
    }

    function conditionalUpdate() {
        if (updateT()) {
            console.log('Update required.');
            updateImg();
        }
    }

    function zoom(delta) {
        depth += delta;
        updateImg();
    }

    function zoomIn() {
        if (depth === MAX_LEVEL) {
            return;
        }
        zoom(1);
    }

    function zoomOut() {
        if (depth === MIN_LEVEL) {
            return;
        }
        zoom(-1);
    }

    function handleDimensionChange() {
        w = $body.width();
        h = $body.height();
        updateT();
    }

    function updateConstrain() {
        if (constrain) {
            $('#mapbody').css({
                'max-height': SAFE_HEIGHT,
                'max-width': SAFE_WIDTH
            });
        } else {
            $('#mapbody').css({
                'max-height': '',
                'max-width': ''
            });
        }
        handleDimensionChange();
    }

    function handleHashParameters() {
        // https://stackoverflow.com/a/2880929/437550
        const hash = window.location.hash.substring(1).split('&');
        for (let i = 0; i < hash.length; i++) {
            const temp = hash[i].split('=');

            if (temp[0] === 'lat') {
                lat = parseFloat(temp[1]);
            } else if (temp[0] === 'lon') {
                lon = parseFloat(temp[1]);
            } else if (temp[0] === 'depth') {
                depth = parseInt(temp[1]);
                console.log("new depth " + depth);
            }
        }
    }

    /* only ran once */
    function loadCookies() {
        const allcookies = document.cookie.replace(/ /g, '').split(';');
        let foundConstrain = false;
        let foundTheme = false;
        for (let i = 0; i < allcookies.length; i++) {
            const kv = allcookies[i].split('=');
            if (kv[0] === 'constrain') {
                constrain = (kv[1] === 'true');
                foundConstrain = true;
                if (constrain === true) {
                    updateConstrain();
                }
            } else if (kv[0] === 'theme') {
                theme = kv[1];
                foundTheme = true;
            }
        }
        if (!foundConstrain) {
            document.cookie = 'constrain=false';
            constrain = false;
        }
        if (!foundTheme) {
            document.cookie = 'theme=default';
            theme = 'default';
        }
        const date = new Date();
        // Expire 7 days from now
        date.setTime(date.getTime() + 604800000);
        document.cookie = 'expires=' + date.toGMTString();
    }

    function setTheme() {
        themeableElements.forEach(function(elem) {
            $(elem).removeClass('cal');
            $(elem).removeClass('solarized');
            $(elem).removeClass('eighties');
            $(elem).addClass(theme);
        });
    }

    function setCookie(key, value) {
        document.cookie = key + '=' + value.toString();
    }

    /* ══════════════════════════════════ ೋღ SETUP ღೋ ════════════════════════════════ */

    map = document.getElementById('map');
    dest = document.getElementById('dest');
    dest.style.visibility = 'hidden';
    loadCookies();
    handleHashParameters();
    setTheme();
    updateImg();
    /* Hide scroll bar */
    $('body').css('overflow', 'hidden');

    /* Make search bar do autocomplete things */
    $('#tags').autocomplete({
        source: search,
        minLength: 2,
        select: function(event, ui) {
            $.get({
                async: true,
                url: search,
                dataType: 'json',
                data: { term: ui.item.value, full: true },
                success: function(data) {
                    removeMarkers();
                    for (let i = 0; i < data.length; i++) {
                        console.log(data[i]);
                        const ele = $('<img/>', {
                            id: "marker_" + data[i].id,
                            src: '../round_marker.gif',
                            class: 'rmarker'
                        });
                        ele.appendTo($('#markers'));
                        markers.push({ 
                            id: data[i].id,
                            name: data[i].name || data[i].display_name || ui.item.value,
                            lat: data[i].lat, 
                            lon: data[i].lon, 
                            element: ele 
                        });
                        
                        // 为标记添加点击事件，显示地点详情
                        ele.on('click', function() {
                            showLocationDetails(data[i].id, data[i].name || data[i].display_name || ui.item.value);
                        });
                    }
                    
                    // 如果只有一个结果，自动显示其详情
                    if (data.length === 1) {
                        showLocationDetails(data[0].id, data[0].name || data[0].display_name || ui.item.value);
                    }
                    
                    updateT();
                },
            });
        }
    });
    setTheme();

    /* ══════════════════════════════════ ೋღ EVENTS ღೋ ════════════════════════════════ */

    $('.ui-autocomplete').mouseenter(function() {
        $('.actions').addClass('active');
    }).mouseleave(function() {
        $('.actions').removeClass('active');
    });

    /* Enables drag functionality */
    $body.on('mousedown', function(event) {
        if (event.which !== 1) {
            return; // ignore non-left clicks
        }
        const startX = event.pageX;
        const startY = event.pageY;
        const startLon = lon;
        const startLat = lat;

        $body.on('mousemove', function(event) {
            const dx = event.pageX - startX;
            const dy = event.pageY - startY;
            lon = startLon - (dx * get_londpp());
            lat = startLat - (dy * get_latdpp());
            updateT();
        });

        $body.on('mouseup', function(event) {
            $body.off('mousemove');
            $body.off('mouseup');
            conditionalUpdate();
        });
    });

    $('.zoomin').click(function() {
        zoomIn();
    });

    $('.zoomout').click(function() {
        zoomOut();
    });

    $('.clear').click(function() {
        $.get({
            async: true,
            url: clear_route,
            success: function() {
                dest.style.visibility = 'hidden';
                $directionsText.html('Double click on the map to set start and end points.');
                updateImg();
            },
        });
    });

    $('.info').click(function() {
        $(this).toggleClass('active');
        $('.settings-container').removeClass('active');
    });
    $('.fa-cog').click(function() {
        $('.settings-container').addClass('active');
        if (constrain) {
            $('#constrain-input').prop('checked', true);
        }
        $('input[name=theme][value=' + theme + ']').prop('checked', true);
        $('.info').removeClass('active');
    });
    $('.close-settings').click(function() {
        $('.settings-container').removeClass('active');
    });

    $('.fa-map-signs').click(function() {
        $('.directions-container').addClass('active');
    });
    $('.close-directions').click(function() {
        $('.directions-container').removeClass('active');
    });
    
    $('.fa-info-circle').click(function() {
        $('.location-details-container').addClass('active');
    });
    
    $('.close-location-details').click(function() {
        $('.location-details-container').removeClass('active');
    });
    // 提交评论按钮点击事件
    $('#submit-comment').click(function() {
        const commentText = $('#comment-text').val().trim();
        const locationId = $(this).data('location-id');
        
        // 检查用户是否已登录
        const cookies = document.cookie.split(';');
        let loggedIn = false;
        let username = '';
        let userId = '';
        
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.startsWith('loggedIn=')) {
                loggedIn = cookie.substring('loggedIn='.length) === 'true';
            } else if (cookie.startsWith('username=')) {
                username = cookie.substring('username='.length);
            } else if (cookie.startsWith('userId=')) {
                userId = cookie.substring('userId='.length);
            }
        }
        
        if (loggedIn && username && userId && commentText && locationId) {
            // 向服务器提交评论
            $.ajax({
                url: '/comments',
                type: 'POST',
                data: {
                    location_id: locationId,
                    user_id: userId,
                    content: commentText
                },
                success: function(response) {
                    if (response.success) {
                        // 评论添加成功，在前端显示
                        addComment(username, commentText);
                        
                        // 清空输入框
                        $('#comment-text').val('');
                    } else {
                        alert('Failed to add comment');
                    }
                },
                error: function() {
                    alert('Server error when adding comment');
                }
            });
        } else if (!loggedIn || !userId) {
            // 显示登录提示
            $('#comment-login-notice').show();
        } else if (!commentText) {
            // 评论内容为空
            alert('Please enter a comment');
        }
    });
    $('body').dblclick(function handler(event) {
        if (route_params.start_lon && route_params.end_lon) {  // finished routing, reset routing
            route_params = {};
            $.get({
                async: true,
                url: clear_route,
                success: function() {
                    dest.style.visibility = 'hidden';
                    $directionsText.html('Double click on the map to set start and end points.');
                    updateImg();
                },
            });
        }
        const offset = $body.offset();
        const viewbounds = get_view_bounds();
        const click_lon = (event.pageX - offset.left) * get_londpp() + viewbounds.ullon;
        const click_lat = (event.pageY - offset.top) * get_latdpp() + viewbounds.ullat;

        if (route_params.start_lon) { // began routing already but not finished
            route_params.end_lon = click_lon;
            route_params.end_lat = click_lat;
            $routeStatus.hide();
            updateRoute();
            dest.style.visibility = 'visible';
            updateImg();
        } else {
            route_params.start_lon = click_lon;
            route_params.start_lat = click_lat;
            $routeStatus.show();
        }
    });

    /* Enables scroll wheel zoom */
    $(window).bind('mousewheel DOMMouseScroll', function(event) {
        if (event.originalEvent.wheelDelta > 0 || event.originalEvent.detail < 0) {
            zoomIn();
        } else {
            zoomOut();
        }
    });

    /* Prevent image dragging */
    $('img').on('dragstart', function(event) { event.preventDefault(); });

    // Allow for window resizing
    window.onresize = function() {
        handleDimensionChange();
        updateImg();
    };

    window.onhashchange = function() {
        handleHashParameters();
        updateImg();
    };

    $('#constrain-input').change(function() {
        constrain = $(this).is(':checked');
        updateConstrain();
        setCookie('constrain', constrain);
        updateImg();
    });

    $('input[type=radio][name=theme]').change(function() {
        theme = this.value;
        setCookie('theme', this.value);
        setTheme();
    });

    /* 显示地点详情 */
    function showLocationDetails(locationId, locationName) {
        // 设置地点名称
        $('#location-name').text(locationName || 'Unknown Location');
        
        // 清空评论列表
        $('#comments-list').html('<div class="no-comments">No comments yet</div>');
        
        // 设置提交按钮的location-id属性
        $('#submit-comment').data('location-id', locationId);
        
        // 显示详情窗口
        $('.location-details-container').addClass('active');
        
        // 从服务器获取评论
        $.ajax({
            url: '/comments',
            type: 'GET',
            data: { location_id: locationId },
            success: function(comments) {
                if (comments && comments.length > 0) {
                    // 清空评论列表
                    $('#comments-list').empty();
                    
                    // 添加所有评论
                    comments.forEach(function(comment) {
                        addComment(comment.username, comment.content);
                    });
                }
                
                // ===== 自动添加评论功能开始 =====
                // 注意：这是一个临时功能，用于测试评论系统
                // 可以通过注释掉以下代码块来禁用此功能
                autoAddComments(locationId);
                // ===== 自动添加评论功能结束 =====
            },
            error: function() {
                console.error('Failed to load comments');
            }
        });
    }
    
    /* 自动添加三条评论的函数 */
    function autoAddComments(locationId) {
        // 三个测试用户的ID和名称
        const testUsers = [
            { id: 4, name: 'Josh' },
            // { id: 2, name: 'zeze' },
            // { id: 3, name: 'jiege' }
        ];
        
        // 三条测试评论
        const testComments = [
            'Have you done it, chia?',
            // 'Smoke Out!',
            // 'I\'m nothing... but a cookie.'
        ];
        
        // 并行发送三个评论请求
        testUsers.forEach(function(user, index) {
            // 使用setTimeout使请求有轻微的时间差，避免完全同时发送
            setTimeout(function() {
                $.ajax({
                    url: '/comments',
                    type: 'POST',
                    data: {
                        location_id: locationId,
                        user_id: user.id,
                        content: testComments[index]
                    },
                    success: function(response) {
                        if (response.success) {
                            // 评论添加成功，在前端显示
                            addComment(user.name, testComments[index]);
                            console.log('自动评论已添加:', user.name, testComments[index]);
                        }
                    },
                    error: function() {
                        console.error('自动添加评论失败');
                    }
                });
            }, index * 300); // 每个请求间隔300毫秒
        });
    }
    
    /* 添加评论到列表 */
    function addComment(username, text) {
        // 移除"没有评论"提示
        $('.no-comments').remove();
        
        // 创建新评论元素
        const commentHtml = `
            <div class="comment">
                <div class="comment-username">${username}</div>
                <div class="comment-text">${text}</div>
            </div>
        `;
        
        // 添加到评论列表
        $('#comments-list').append(commentHtml);
    }
    
    /* 登录功能相关 */
    // 打开登录模态框
    $('#login-button').click(function() {
        $('#login-modal').css('display', 'block');
    });
    
    // 关闭登录模态框
    $('.close-modal').click(function() {
        $('#login-modal').css('display', 'none');
        $('#login-error').css('display', 'none');
    });
    
    // 点击模态框外部关闭模态框
    $(window).click(function(event) {
        if (event.target == document.getElementById('login-modal')) {
            $('#login-modal').css('display', 'none');
            $('#login-error').css('display', 'none');
        }
    });
    
    // 提交登录表单
    $('#submit-login').click(function() {
        const username = $('#login-username').val().trim();
        const password = $('#login-password').val().trim();
        
        if (username && password) {
            // 向服务器发送登录请求
            $.ajax({
                url: '/login',
                type: 'POST',
                data: { username: username, password: password },
                success: function(response) {
                    if (response.success) {
                        // 登录成功
                        $('#login-modal').css('display', 'none');
                        $('#login-button').text(response.username);
                        $('#login-button').addClass('logged-in');
                        
                        // 清空表单
                        $('#login-username').val('');
                        $('#login-password').val('');
                        $('#login-error').css('display', 'none');
                        
                        // 保存登录状态到cookie
                        document.cookie = 'loggedIn=true; path=/';
                        document.cookie = 'username=' + response.username + '; path=/';
                        document.cookie = 'userId=' + response.userId + '; path=/';
                    } else {
                        // 登录失败
                        $('#login-error').text(response.message || '登录失败');
                        $('#login-error').css('display', 'block');
                    }
                },
                error: function() {
                    $('#login-error').text('服务器错误，请稍后再试');
                    $('#login-error').css('display', 'block');
                }
            });
        } else {
            // 表单验证失败
            $('#login-error').text('请输入用户名和密码');
            $('#login-error').css('display', 'block');
        }
    });
    
    // 提交注册表单
    $('#submit-register').click(function() {
        const username = $('#login-username').val().trim();
        const password = $('#login-password').val().trim();
        
        if (username && password) {
            // 向服务器发送注册请求
            $.ajax({
                url: '/register',
                type: 'POST',
                data: { username: username, password: password },
                success: function(response) {
                    if (response.success) {
                        // 注册成功
                        $('#login-error').css('display', 'none');
                        $('#login-error').removeClass('error-message').addClass('success-message').text('注册成功！请登录');
                        $('#login-error').css('display', 'block');
                        $('#login-error').css('color', '#4CAF50');
                        
                        // 清空密码字段
                        $('#login-password').val('');
                    } else {
                        // 注册失败
                        $('#login-error').removeClass('success-message').addClass('error-message');
                        $('#login-error').text(response.message || '注册失败');
                        $('#login-error').css('display', 'block');
                        $('#login-error').css('color', '#B71C1C');
                    }
                },
                error: function() {
                    $('#login-error').removeClass('success-message').addClass('error-message');
                    $('#login-error').text('服务器错误，请稍后再试');
                    $('#login-error').css('display', 'block');
                    $('#login-error').css('color', '#B71C1C');
                }
            });
        } else {
            // 表单验证失败
            $('#login-error').removeClass('success-message').addClass('error-message');
            $('#login-error').text('请输入用户名和密码');
            $('#login-error').css('display', 'block');
            $('#login-error').css('color', '#B71C1C');
        }
    });
    
    // 检查登录状态
    function checkLoginStatus() {
        const cookies = document.cookie.split(';');
        let loggedIn = false;
        let username = '';
        
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.startsWith('loggedIn=')) {
                loggedIn = cookie.substring('loggedIn='.length) === 'true';
            } else if (cookie.startsWith('username=')) {
                username = cookie.substring('username='.length);
            }
        }
        
        if (loggedIn && username) {
            $('#login-button').text(username);
            $('#login-button').addClass('logged-in');
        }
    }
    
    // 页面加载时检查登录状态
    checkLoginStatus();
    
    /* Keyboard navigation callbacks */
    document.onkeydown = function(e) {
        const delta = base_move_delta;
        switch (e.keyCode) {
            case 37: //left
                lon -= delta * get_londpp();
                conditionalUpdate();
                break;
            case 38: //up
                lat -= delta * get_latdpp();
                conditionalUpdate();
                break;
            case 39: //right
                lon += delta * get_londpp();
                conditionalUpdate();
                break;
            case 40: //down
                lat += delta * get_latdpp();
                conditionalUpdate();
                break;
            case 189: //minus
                zoomOut();
                break;
            case 187: //equals/plus
                zoomIn();
                break;
        }
    };
});