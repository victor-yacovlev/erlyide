%%
 % Copyright 2016 Victor Yacovlev <v.yacovlev@gmail.com>
 %
 %    Licensed under the Apache License, Version 2.0 (the "License");
 %    you may not use this file except in compliance with the License.
 %    You may obtain a copy of the License at
 %
 %        http://www.apache.org/licenses/LICENSE-2.0
 %
 %    Unless required by applicable law or agreed to in writing, software
 %    distributed under the License is distributed on an "AS IS" BASIS,
 %    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 %    See the License for the specific language governing permissions and
 %    limitations under the License.
 %

-module(erlide_syntax_check).

%% API
-export([check_for_errors_and_warnings/2, helper_info/0]).

-type token() :: erl_scan:token().
-type level() :: 'error' | 'warning'.
-type location() :: integer() | {integer(), integer()}.
-type message() :: nonempty_string().
-type error_information() :: { location(), level(), message() }.

-spec check_for_errors_and_warnings(string(), string()) -> [error_information()].
check_for_errors_and_warnings(Input, FileName) ->
  ScanResult = erl_scan:string(Input),
  case ScanResult of
    {error, {ErrLoc, ErrMod, ErrDesc}, _} -> [
      {ErrLoc, error, ErrMod:format_error(ErrDesc)}
    ];
    {ok, Tokens, _} -> process_tokens_check(Input, Tokens, FileName)
  end.


-spec process_tokens_check(string(), [token()], string()) -> [error_information()].
process_tokens_check(Input, Tokens, FileName) ->
  process_base_check(Tokens, FileName) ++
  process_extra_check(Input, Tokens, FileName).


-spec process_base_check([token()], string()) -> [error_information()].
process_base_check(Tokens, _FileName) ->
  ParseResult = erl_parse:parse_form(Tokens),
  case ParseResult of
    {ok, _} -> [];
    {Level, {ErrLoc, ErrMod, ErrDesc}} -> [
      {ErrLoc, Level, lists:flatten(ErrMod:format_error(ErrDesc))}
    ]
  end.

-spec process_extra_check(string(), [token()], string()) -> [error_information()].
process_extra_check(_Input, _Tokens, _FileName) ->
  []. % TODO implement me some time

-type name() :: atom().
-type start_function() :: 'none' | atom().
-type version_major() :: integer().
-type version_minor() :: integer().
-type version_patch() :: integer().
-type version() :: {version_major(), version_minor(), version_patch()}.

-spec helper_info() -> {ok, name(), version(), start_function()}.
helper_info() ->  {ok, ?MODULE, {0,1,0}, none}.